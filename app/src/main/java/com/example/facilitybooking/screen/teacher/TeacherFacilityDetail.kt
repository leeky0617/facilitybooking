package com.example.facilitybooking

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import java.time.LocalDate

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TeacherFacilityDetailScreen(navController: NavController, facilityId: String) {
    // 1. State
    var facility by remember { mutableStateOf<Facility?>(null) }
    var bookedPax by remember { mutableIntStateOf(0) }

    val lifecycleOwner = LocalLifecycleOwner.current

    // 2. Fetch Data Function
    fun loadData() {
        FirestoreRepository.getFacilityById(facilityId) {
            facility = it
        }

        val today = LocalDate.now().toString()
        FirestoreRepository.getReservationsByDate(facilityId, today) { reservations ->
            val activeReservations = reservations.filter { it.status != "CANCELLED" }
            bookedPax = activeReservations.sumOf { it.pax }
        }
    }

    // 3. Auto-Refresh on Return
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                loadData()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (facility == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    val totalDailyCapacity = facility!!.totalCapacity * 6
    Column(modifier = Modifier.fillMaxSize()) {

        // --- TOP IMAGE BANNER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(300.dp)
                .background(Color.Gray)
        ) {
            // 1. Decode Image from String
            val bitmap = remember(facility!!.imageBase64) {
                if (facility!!.imageBase64.isNotEmpty()) {
                    ImageUtils.stringToBitmap(facility!!.imageBase64)
                } else null
            }

            // 2. Display Image
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = facility!!.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                // Dark Overlay for readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f))
                )
            } else {
                // No Image Text
                Box(Modifier.align(Alignment.Center)) {
                    Text(facility!!.name, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            // 3. Back Button
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier
                    .padding(16.dp)
                    .background(Color.White, RoundedCornerShape(8.dp))
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.Black)
            }
        }

        // --- DETAILS ---
        Column(modifier = Modifier.padding(24.dp)) {
            Text(facility!!.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))
            Text(facility!!.description, fontSize = 14.sp, color = Color.Gray, lineHeight = 20.sp)
            Spacer(modifier = Modifier.height(24.dp))

            // Capacity
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Daily Capacity: ", fontSize = 16.sp, fontWeight = FontWeight.Medium)

                val capacityColor = if (bookedPax >= totalDailyCapacity) Color.Red else Color.Black

                Text(
                    text = "$bookedPax / $totalDailyCapacity",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = capacityColor
                )

                Text(" (Today)", fontSize = 14.sp, color = Color.Gray, modifier = Modifier.padding(start = 4.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Opening Hours: ${facility!!.openingHours}", fontSize = 16.sp, fontWeight = FontWeight.Medium)

            Spacer(modifier = Modifier.weight(1f))

            // Reserve Button
            Button(
                onClick = { navController.navigate("teacher_reserve/${facility!!.id}") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D9CEC))
            ) {
                Text("Reserve", fontSize = 18.sp)
            }
        }
    }
}