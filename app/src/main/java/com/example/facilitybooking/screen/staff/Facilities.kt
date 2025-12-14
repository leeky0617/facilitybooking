package com.example.facilitybooking

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCodeScanner
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Image
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.asImageBitmap

@Composable
fun FacilitiesScreen(navController: NavController) {
    var facilities by remember { mutableStateOf<List<Facility>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Reload data every time screen appears
                isLoading = true
                FirestoreRepository.getFacilities { result ->
                    facilities = result
                    isLoading = false
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_facility") },
                containerColor = Color(0xFF009688)
            ) {
                Text("+", color = Color.White, fontSize = 28.sp)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFF2F4F4))
                .padding(16.dp)
                .padding(padding)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconBox(
                    icon = {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profile",
                            tint = Color.Gray
                        )
                    },
                    onClick = { navController.navigate("staff_profile") }
                )
                IconBox(
                    icon = {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = "Scan",
                            tint = Color.Gray
                        )
                    },
                    onClick = { navController.navigate("qr_scan") }
                )
            }

            Spacer(Modifier.height(16.dp))

            Text("Mon, 22/12/2025", color = Color.DarkGray, fontSize = 14.sp)
            Text("Facilities Management", fontWeight = FontWeight.Bold, fontSize = 24.sp)

            Spacer(Modifier.height(20.dp))

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(facilities) { facility ->
                        // Pass the whole facility object
                        FacilityCard(facility = facility) {
                            navController.navigate("facilityDetail/${facility.id}")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IconBox(icon: @Composable () -> Unit, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(50.dp)
            .clip(RoundedCornerShape(15.dp))
            .background(Color(0xFFE0ECEF))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        icon()
    }
}

@Composable
fun FacilityCard(facility: Facility, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(25.dp))
            .background(Color(0xFFE6E6E6))
            .padding(10.dp)
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .height(130.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(25.dp))
                .background(Color(0xFFCCCCCC)),

                    contentAlignment = Alignment.Center
        ) {
            // --- DECODE BASE64 STRING ---
            val bitmap = remember(facility.imageBase64) {
                if (facility.imageBase64.isNotEmpty()) ImageUtils.stringToBitmap(facility.imageBase64) else null
            }

            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = facility.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No Image", color = Color.Gray, fontSize = 12.sp)
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(facility.name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}