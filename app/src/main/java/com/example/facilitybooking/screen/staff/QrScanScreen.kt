package com.example.facilitybooking

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

@androidx.annotation.OptIn(ExperimentalGetImage::class)
@Composable
fun QrScanScreen(navController: NavController) {
    val context = LocalContext.current

    // State to handle the result
    var scannedCode by remember { mutableStateOf<String?>(null) }

    // Permission State
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { granted -> hasPermission = granted }
    )

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        // --- 1. IF NO CODE SCANNED YET, SHOW CAMERA ---
        if (scannedCode == null) {
            if (hasPermission) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()

                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(surfaceProvider)
                                }

                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()

                                imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                                    val mediaImage = imageProxy.image

                                    // Check scannedCode again here to stop processing frames if we found one
                                    if (mediaImage != null && scannedCode == null) {
                                        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

                                        BarcodeScanning.getClient().process(image)
                                            .addOnSuccessListener { barcodes ->
                                                for (barcode in barcodes) {
                                                    barcode.rawValue?.let { code ->
                                                        // --- CODE FOUND! ---
                                                        if (scannedCode == null) {
                                                            scannedCode = code

                                                            vibratePhone(context)

                                                            // --- YOUR LOGIC HERE ---
                                                            val reservationId = if (code.startsWith("BOOKING:")) {
                                                                code.removePrefix("BOOKING:")
                                                            } else {
                                                                code
                                                            }

                                                            // Navigate immediately
                                                            // We use MainLooper to ensure navigation happens on UI thread
                                                            android.os.Handler(android.os.Looper.getMainLooper()).post {
                                                                Toast.makeText(context, "Scanning...", Toast.LENGTH_SHORT).show()
                                                                navController.navigate("staff_reservation_detail/$reservationId") {
                                                                    popUpTo("qr_scan") { inclusive = true }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                            .addOnCompleteListener { imageProxy.close() }
                                    } else {
                                        imageProxy.close()
                                    }
                                }

                                try {
                                    cameraProvider.unbindAll()
                                    cameraProvider.bindToLifecycle(
                                        context as androidx.lifecycle.LifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview,
                                        imageAnalysis
                                    )
                                } catch(e: Exception) {
                                    Log.e("Camera", "Binding failed", e)
                                }
                            }, ContextCompat.getMainExecutor(ctx))
                        }
                    },
                    Modifier.fillMaxSize()
                )

                // Overlay Text
                Box(Modifier.align(Alignment.Center)) {
                    Text(
                        "Scan QR Code",
                        color = Color.White.copy(alpha = 0.8f),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }
            } else {
                Text(
                    "Camera Permission Needed",
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
        // --- 2. FALLBACK (In case navigation takes a split second) ---
        else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        // Back Button (Always visible)
        Box(
            Modifier
                .align(Alignment.TopStart)
                .padding(20.dp)
                .size(40.dp)
                .clip(CircleShape)
                .background(Color.White)
                .clickable { navController.popBackStack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.Black)
        }
    }
}

// Helper function to vibrate phone
fun vibratePhone(context: Context) {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    if (Build.VERSION.SDK_INT >= 26) {
        vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
    } else {
        vibrator.vibrate(100)
    }
}