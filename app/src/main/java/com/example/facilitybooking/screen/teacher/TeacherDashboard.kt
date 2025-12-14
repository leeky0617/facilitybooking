package com.example.facilitybooking

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.navigation.NavController
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Image
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TeacherDashboard(navController: NavController) {
    // 1. State
    var facilities by remember { mutableStateOf<List<Facility>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf(FacilityCategory.INDOOR) }
    var isLoading by remember { mutableStateOf(true) }
    var hasTodayReservation by remember { mutableStateOf(false) }

    // 2. Fetch Data with Auto-Refresh
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                isLoading = true
                FirestoreRepository.getFacilities { result ->
                    // --- CORRECTED: Direct Assignment ---
                    // No filter needed because drafts are gone
                    facilities = result
                    isLoading = false
                }

                val today = LocalDate.now().toString()
                FirestoreRepository.getUserReservations { reservations ->
                    hasTodayReservation = reservations.any {
                        it.date == today && (it.status == "PENDING" || it.status == "IN_USE")
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // 3. Filter by Category
    val filteredFacilities = facilities.filter {
        it.category.trim().equals(selectedCategory.name, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- SIDEBAR ---
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(90.dp)
                .background(Color(0xFFA8D4E6))
                .padding(vertical = 16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Spacer(modifier = Modifier.height(10.dp))
                Box(contentAlignment = Alignment.TopEnd) {
                    IconButton(onClick = { navController.navigate("teacher_notification") }) {
                        Icon(Icons.Default.Notifications, "Notifications", tint = Color.White)
                    }

                    if (hasTodayReservation) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .offset(x = (-4).dp, y = 12.dp)
                                .background(Color.Red, CircleShape)
                                .border(2.dp, Color(0xFFA8D4E6), CircleShape)
                        )
                    }
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CategoryLabel("Indoor", selectedCategory == FacilityCategory.INDOOR) { selectedCategory = FacilityCategory.INDOOR }
                Spacer(modifier = Modifier.height(48.dp))
                CategoryLabel("Outdoor", selectedCategory == FacilityCategory.OUTDOOR) { selectedCategory = FacilityCategory.OUTDOOR }
                Spacer(modifier = Modifier.height(48.dp))
                CategoryLabel("Education", selectedCategory == FacilityCategory.EDUCATION) { selectedCategory = FacilityCategory.EDUCATION }
            }

            IconButton(onClick = { navController.navigate("teacher_profile") }) {
                Icon(Icons.Default.AccountCircle, "Profile", tint = Color.White)
            }
        }

        // --- MAIN CONTENT ---
        Column(modifier = Modifier.fillMaxSize().padding(start = 90.dp)) {
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                modifier = Modifier.fillMaxWidth().padding(16.dp).clickable { navController.navigate("teacher_search") },
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFA8D4E6)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Menu, "Menu", tint = Color.Gray)
                    Spacer(Modifier.width(8.dp))
                    Text("Search facilities...", color = Color.Gray, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Search, "Search", tint = Color.Gray)
                }
            }

            Column(modifier = Modifier.padding(16.dp)) {
                val today = LocalDate.now()
                val formatter = DateTimeFormatter.ofPattern("EEE, dd/MM/yyyy", Locale.ENGLISH)
                Text(today.format(formatter), fontSize = 14.sp, color = Color.Gray)
                Text("Facilities List", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (filteredFacilities.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No facilities available in this category.")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredFacilities) { facility ->
                        TeacherFacilityCard(
                            facility = facility,
                            onClick = { navController.navigate("teacher_facility/${facility.id}") }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CategoryLabel(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Text(
        text = text,
        fontSize = 14.sp,
        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        color = if (isSelected) Color.Black else Color.White,
        modifier = Modifier.clickable(onClick = onClick).padding(8.dp)
    )
}

@Composable
fun TeacherFacilityCard(facility: Facility, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp)
    ) {
        // --- PARENT BOX (Holds Image + Bottom Bar) ---
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- 1. IMAGE LAYER ---
            // Decode Base64 String to Bitmap
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
                // Fallback Grey Background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.LightGray),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Outlined.Image, null, tint = Color.DarkGray)
                        Text("No Image", color = Color.DarkGray, fontSize = 12.sp)
                    }
                }
            }

            // --- 2. BOTTOM INFO BAR LAYER ---
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter) // This requires being inside a Box
                    .fillMaxWidth(),
                color = Color.White
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = facility.name,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        // Show Category
                        Text(
                            text = facility.category,
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }

                    // Plus Icon
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Book",
                        tint = Color.Black
                    )
                }
            }
        }
    }
}