package com.example.facilitybooking

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.firestore.auth.User

// Colors from your image
private val LightGreenBtn = Color(0xFFA6D9A6)
private val LightYellowBtn = Color(0xFFEEE8AA) // Or 0xFFD9D9A6
private val HeaderBlue = Color(0xFFF0F8FF) // Very light blue for "Badminton Court" bg

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun StaffReservationDetailScreen(
    navController: NavController,
    reservationId: String
) {
    val context = LocalContext.current

    // State
    var reservation by remember { mutableStateOf<Reservation?>(null) }
    var teacher by remember { mutableStateOf<FacilityUser?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Fetch Data
    LaunchedEffect(reservationId) {
        FirestoreRepository.getReservationById(reservationId) { res ->
            if (res != null) {
                reservation = res
                // Once we have reservation, fetch the Teacher details
                FirestoreRepository.getUserById(res.userId) { user ->
                    teacher = user
                    isLoading = false
                }
            } else {
                Toast.makeText(context, "Reservation not found", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
        }
    }

    if (isLoading || reservation == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {

        // --- 1. TOP BAR ---
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = reservation!!.getDateTimeString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        // --- 2. BOOKING ID ---
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text("Booking ID", color = Color.Gray, fontSize = 14.sp)
            Text(reservation!!.bookingId, color = Color.Gray, fontSize = 14.sp)
        }

        // --- 3. FACILITY TITLE BANNER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(HeaderBlue)
                .padding(vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = reservation!!.facilityName,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        // --- 4. PROFILE & DETAILS ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Picture Placeholder
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFA8D4E6)) // Light Blue Circle
                    .border(2.dp, Color.White, CircleShape)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Details Table
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Teacher Profile", fontSize = 16.sp, color = Color.Black)
                HorizontalDivider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(vertical = 8.dp))

                DetailRow("Teacher Name", teacher?.name ?: "Loading...")
                DetailRow("Teacher Email", teacher?.email ?: "Loading...")
                DetailRow("Facilities Type", "Indoor") // Or fetch facility.category
                DetailRow("Facilities No", "Court ${reservation!!.courtNumbers.joinToString(",")}")
                DetailRow("PAX", reservation!!.pax.toString())
                DetailRow("Status", reservation!!.status)
            }

            Spacer(modifier = Modifier.height(40.dp))

            // --- 5. ACTION BUTTONS ---
            Button(
                onClick = {
                    FirestoreRepository.updateReservationStatus(reservationId, "IN_USE") {
                        Toast.makeText(context, "Checked In Successfully", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LightGreenBtn)
            ) {
                Text("Check In", color = Color.Black, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    FirestoreRepository.updateReservationStatus(reservationId, "Completed") {
                        Toast.makeText(context, "Checked Out Successfully", Toast.LENGTH_SHORT).show()
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LightYellowBtn)
            ) {
                Text("Check Out", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 15.sp, color = Color.DarkGray)
        Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.Black)
    }
    HorizontalDivider(color = Color(0xFFF5F5F5), thickness = 1.dp)
}