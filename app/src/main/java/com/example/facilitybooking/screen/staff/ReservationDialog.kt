package com.example.facilitybooking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun ReservationDialog(
    reservation: Reservation, // Accepts full object
    onDismiss: () -> Unit
) {
    var teacherName by remember { mutableStateOf("Loading...") }
    var teacherEmail by remember { mutableStateOf("") }

    // Fetch Teacher Info
    LaunchedEffect(reservation.userId) {
        FirestoreRepository.getUserById(reservation.userId) { user ->
            if (user != null) {
                teacherName = user.name
                teacherEmail = user.email
            } else {
                teacherName = "Unknown"
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), Arrangement.End) {
                    Icon(Icons.Default.Close, "Close", Modifier.clickable { onDismiss() })
                }

                Text("Reservation Details", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(Modifier.height(20.dp))

                // Details
                Text(reservation.facilityName, fontSize = 16.sp, color = Color.Black)
                Spacer(Modifier.height(6.dp))
                Text("Court ${reservation.courtNumbers.joinToString(",")}", fontSize = 14.sp, color = Color.Black)
                Spacer(Modifier.height(6.dp))
                Text(reservation.getDateTimeString(), fontSize = 14.sp, color = Color.Black)
                Spacer(Modifier.height(6.dp))
                Text("${reservation.pax} Pax", fontSize = 14.sp, color = Color.Black)

                Spacer(Modifier.height(20.dp))

                // Booked By
                Text("Booked by:", fontSize = 12.sp, color = Color.Gray)
                Text(teacherName, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(teacherEmail, fontSize = 12.sp, color = Color.Gray)

                Spacer(Modifier.height(20.dp))

                // Status Badge
                val badgeColor = when (reservation.status) {
                    "PENDING" -> Color(0xFFD9D9A6)
                    "IN_USE" -> Color(0xFFA6D9A6)
                    "Completed" -> Color(0xFFC6A6D9)
                    "CANCELLED" -> Color(0xFFD9A6A6)
                    else -> Color.LightGray
                }

                Box(Modifier.clip(RoundedCornerShape(50)).background(badgeColor).padding(horizontal = 40.dp, vertical = 8.dp)) {
                    Text(reservation.status, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}