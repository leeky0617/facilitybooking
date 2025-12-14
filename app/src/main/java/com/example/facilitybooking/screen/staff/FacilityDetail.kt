package com.example.facilitybooking

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest

@Composable
fun FacilityDetailScreen(
    navController: NavController,
    facilityName: String // This is actually the Facility ID passed from navigation
) {
    val context = LocalContext.current

    // State to hold the fetched data
    var facility by remember { mutableStateOf<Facility?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Fetch Facility Data from Firestore when screen loads
    LaunchedEffect(facilityName) {
        FirestoreRepository.getFacilityById(facilityName) { fetchedData ->
            facility = fetchedData
        }
    }

    // Show Loading Spinner until data arrives
    if (facility == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Generate court list based on the count (e.g., Court 1, Court 2...)
    val courts = List(facility!!.courtCount) { "Court ${it + 1}" }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(20.dp)
    ) {

        // --- 1. TOP BAR (Back + Delete) ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Back Button
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFCDE8F0))
                    .clickable { navController.popBackStack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }

            // Delete Button
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFB4AB)) // Light Red
                    .clickable { showDeleteDialog = true },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Facility",
                    tint = Color.Red
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // --- 2. HEADER TEXT ---
        Text(
            text = "Facility Details",
            fontSize = 14.sp,
            color = Color.Gray
        )
        Text(
            text = "Facilities Management",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )

        Spacer(modifier = Modifier.height(30.dp))

        // --- 3. DETAILS SECTION ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // --- IMAGE BOX (Clickable to Edit) ---
            // Parent Box holds both the Image and the Edit Overlay
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(130.dp)
                    .clickable {
                        navController.navigate("edit_facility/${facility!!.id}")
                    }
            ) {
                // 1. The Main Image Circle
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(Color.LightGray),
                            contentAlignment = Alignment.Center
                ) {
                    val bitmap = remember(facility!!.imageBase64) {
                        if (facility!!.imageBase64.isNotEmpty()) {
                            ImageUtils.stringToBitmap(facility!!.imageBase64)
                        } else null
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Facility Image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Fallback Icon
                        Icon(
                            imageVector = Icons.Outlined.Image,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                // 2. The Edit Overlay (Small semi-transparent circle on top)
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x80000000)), // Semi-transparent black
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit, // Or Icons.Default.Edit
                        contentDescription = "Edit",
                        tint = Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.width(20.dp))

            // Text Details with Edit Links
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {

                DetailItem(
                    label = "Facility Name",
                    value = facility!!.name,
                    onEditClick = { navController.navigate("edit_facility/${facility!!.id}") }
                )

                DetailItem(
                    label = "Facility Type",
                    value = facility!!.category,
                    onEditClick = { navController.navigate("edit_facility/${facility!!.id}") }
                )

                DetailItem(
                    label = "No of court",
                    value = facility!!.courtCount.toString(),
                    onEditClick = { navController.navigate("edit_facility/${facility!!.id}") }
                )

                DetailItem(
                    label = "Capacity",
                    value = "${facility!!.totalCapacity} Pax",
                    onEditClick = { navController.navigate("edit_facility/${facility!!.id}") }
                )
            }
        }

        Spacer(modifier = Modifier.height(30.dp))

        // --- 4. DIVIDER ---
        HorizontalDivider(thickness = 1.dp, color = Color(0xFFEEEEEE))
        Spacer(modifier = Modifier.height(20.dp))

        // --- 5. TIMETABLE BUTTON ---
        Button(
            onClick = {
                navController.navigate("timetable")
            },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCDE8F0)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("View Overall Timetable", color = Color.Black, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider(thickness = 1.dp, color = Color(0xFFEEEEEE))
        Spacer(modifier = Modifier.height(30.dp))

        // --- 6. COURTS GRID ---
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            horizontalArrangement = Arrangement.spacedBy(15.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(courts) { courtName ->
                Box(
                    modifier = Modifier
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(15.dp))
                        .background(Color(0xFFCDE8F0))
                        .clickable {
                            // Pass the court name to the schedule screen
                            navController.navigate("court_schedule/$courtName")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = courtName,
                        fontSize = 14.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }

    // --- DELETE CONFIRMATION DIALOG ---
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Facility?") },
            text = { Text("Are you sure you want to delete '${facility!!.name}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        // Call Repository to delete
                        FirestoreRepository.deleteFacility(
                            facilityId = facility!!.id,
                            onSuccess = {
                                Toast.makeText(context, "Facility Deleted", Toast.LENGTH_SHORT).show()
                                showDeleteDialog = false
                                navController.popBackStack() // Go back to dashboard
                            },
                            onFailure = { error ->
                                Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
                                showDeleteDialog = false
                            }
                        )
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(
                    onClick = { showDeleteDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Cancel")
                }
            },
            containerColor = Color.White
        )
    }
}

// --- HELPER COMPONENT ---
@Composable
fun DetailItem(
    label: String,
    value: String,
    onEditClick: () -> Unit
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = label, fontSize = 12.sp, color = Color.Gray)
            Spacer(modifier = Modifier.width(5.dp))

            // Edit Icon
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit",
                modifier = Modifier
                    .size(16.dp)
                    .clickable { onEditClick() }, // Makes the icon clickable
                tint = Color.Gray
            )
        }
        Text(text = value, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.Black)
    }
}