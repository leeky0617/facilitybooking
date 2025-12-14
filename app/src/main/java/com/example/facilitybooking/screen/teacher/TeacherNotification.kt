package com.example.facilitybooking

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PendingActions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TeacherNotificationScreen(navController: NavController) {
    var reservations by remember { mutableStateOf<List<Reservation>>(emptyList()) }

    // Helper function to check if reservation has expired
    fun isReservationExpired(reservation: Reservation): Boolean {
        return try {
            val bookingDate = LocalDate.parse(reservation.date)
            val formatter = DateTimeFormatter.ofPattern("hh:mm a")
            val endTime = LocalTime.parse(reservation.timeSlot.endTime, formatter)
            val bookingDateTime = LocalDateTime.of(bookingDate, endTime)
            LocalDateTime.now().isAfter(bookingDateTime)
        } catch (e: Exception) {
            false
        }
    }

    LaunchedEffect(Unit) {
        FirestoreRepository.getUserReservations { list ->
            // Auto-update expired PENDING reservations to NO_SHOW
            list.forEach { reservation ->
                if (reservation.status == "PENDING" && isReservationExpired(reservation)) {
                    FirestoreRepository.updateReservationStatus(reservation.id, "NO_SHOW") {}
                }
            }

            // Filter: Only show PENDING and IN_USE that haven't expired
            val filtered = list.filter { reservation ->
                val isExpired = isReservationExpired(reservation)

                when (reservation.status) {
                    "PENDING" -> !isExpired // Only show PENDING if not expired
                    "IN_USE" -> true // Always show IN_USE
                    else -> false // Don't show other statuses
                }
            }

            // Sort by custom logic
            val today = LocalDate.now()
            reservations = filtered.sortedWith(compareBy<Reservation> { reservation ->
                // Priority 1: Today's bookings come first (0), future bookings come second (1)
                try {
                    val resDate = LocalDate.parse(reservation.date)
                    if (resDate == today) 0 else 1
                } catch (e: Exception) {
                    1
                }
            }.thenBy { reservation ->
                // Priority 2: IN_USE comes first (0), PENDING comes second (1)
                when (reservation.status) {
                    "IN_USE" -> 0
                    "PENDING" -> 1
                    else -> 2
                }
            }.thenBy { reservation ->
                // Priority 3: Sort by date (earliest first)
                try {
                    LocalDate.parse(reservation.date)
                } catch (e: Exception) {
                    LocalDate.MAX
                }
            }.thenBy { reservation ->
                // Priority 4: Sort by time slot (earliest first)
                try {
                    val timeStr = reservation.timeSlot.startTime.split(":")[0].split(" ")[0]
                    timeStr.toIntOrNull() ?: 99
                } catch (e: Exception) {
                    99
                }
            })
        }
    }

    val today = LocalDate.now()

    Column(modifier = Modifier.fillMaxSize().background(Color.White)) {
        Spacer(modifier = Modifier.height(20.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text(
                "Notifications",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Info text
        Text(
            "Upcoming and active bookings",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // List
        if (reservations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.PendingActions,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        "No upcoming bookings",
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Your active and pending bookings will appear here",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(reservations) { res ->
                    val isInUse = res.status == "IN_USE"
                    val resDate = try { LocalDate.parse(res.date) } catch (e: Exception) { null }
                    val isToday = resDate == today

                    // Red alert for today's reservations
                    val bgColor = if (isToday) Color(0xFFFFEBEE) else if (isInUse) Color(0xFFE8F5E9) else Color(0xFFE0F7FA)
                    val iconColor = if (isToday) Color(0xFFE53935) else if (isInUse) Color(0xFF4CAF50) else Color(0xFF5D9CEC)
                    val statusText = if (isInUse) "In Use" else "Upcoming"

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                navController.navigate("reservation_detail/${res.id}")
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Icon Box
                            Box(
                                modifier = Modifier
                                    .size(50.dp)
                                    .background(Color.White, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isInUse) Icons.Default.CheckCircle else Icons.Default.AccessTime,
                                    contentDescription = null,
                                    tint = iconColor,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            Spacer(Modifier.width(16.dp))

                            // Details
                            Column(modifier = Modifier.weight(1f)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        res.facilityName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.Black
                                    )

                                    // Status Badge
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = iconColor.copy(alpha = 0.2f)
                                    ) {
                                        Text(
                                            if (isToday) "TODAY!" else statusText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = iconColor,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(Modifier.height(4.dp))
                                Text(
                                    res.getDateTimeString(),
                                    fontSize = 13.sp,
                                    color = Color.DarkGray
                                )

                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Court ${res.courtNumbers.joinToString(",")} • ${res.pax} pax",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}