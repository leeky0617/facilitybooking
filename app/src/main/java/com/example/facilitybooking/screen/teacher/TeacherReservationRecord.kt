package com.example.facilitybooking

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.EventBusy
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
fun TeacherReservationRecordScreen(navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var reservations by remember { mutableStateOf<List<Reservation>>(emptyList()) }

    // Helper functions
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

    fun getEffectiveStatus(reservation: Reservation): String {
        return if (reservation.status == "PENDING" && isReservationExpired(reservation)) {
            "NO_SHOW"
        } else {
            reservation.status
        }
    }

    LaunchedEffect(Unit) {
        FirestoreRepository.getUserReservations { list ->
            // Auto-update expired reservations
            list.forEach { reservation ->
                if (reservation.status == "PENDING" && isReservationExpired(reservation)) {
                    FirestoreRepository.updateReservationStatus(reservation.id, "NO_SHOW") {}
                }
            }

            // Filter: Only Completed, Cancelled, and NO_SHOW
            reservations = list.filter {
                val status = getEffectiveStatus(it)
                status == "Completed" || status == "CANCELLED" || status == "NO_SHOW"
            }
        }
    }

    // Filter based on tab and sort (latest first)
    val filtered = reservations.filter {
        val effectiveStatus = getEffectiveStatus(it)
        when (selectedTab) {
            0 -> effectiveStatus == "Completed"
            1 -> effectiveStatus == "CANCELLED"
            2 -> effectiveStatus == "NO_SHOW"
            else -> false
        }
    }.sortedWith(compareByDescending<Reservation> { reservation ->
        try {
            LocalDate.parse(reservation.date)
        } catch (e: Exception) {
            LocalDate.MIN
        }
    }.thenByDescending { reservation ->
        try {
            val timeStr = reservation.timeSlot.startTime.split(":")[0].split(" ")[0]
            timeStr.toIntOrNull() ?: 0
        } catch (e: Exception) {
            0
        }
    })

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Spacer(Modifier.height(30.dp))

        // Header
        Row(
            Modifier.fillMaxWidth().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
            }
            Text(
                "Reservation Record",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.White,
            contentColor = Color(0xFF5D9CEC)
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 }
            ) {
                Text(
                    "Completed",
                    Modifier.padding(16.dp),
                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                )
            }
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 }
            ) {
                Text(
                    "Cancelled",
                    Modifier.padding(16.dp),
                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                )
            }
            Tab(
                selected = selectedTab == 2,
                onClick = { selectedTab = 2 }
            ) {
                Text(
                    "Missing",
                    Modifier.padding(16.dp),
                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal
                )
            }
        }

        // List
        if (filtered.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = when (selectedTab) {
                            0 -> Icons.Default.CheckCircle
                            1 -> Icons.Default.Cancel
                            else -> Icons.Default.EventBusy
                        },
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        when (selectedTab) {
                            0 -> "No completed bookings"
                            1 -> "No cancelled bookings"
                            else -> "No missed bookings"
                        },
                        fontSize = 16.sp,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filtered) { res ->
                    val effectiveStatus = getEffectiveStatus(res)
                    val isCompleted = effectiveStatus == "Completed"
                    val isNoShow = effectiveStatus == "NO_SHOW"

                    val bgColor = when {
                        isCompleted -> Color(0xFFF3E5F5)
                        isNoShow -> Color(0xFFFFF3E0)
                        else -> Color(0xFFFFEBEE)
                    }
                    val statusColor = when {
                        isCompleted -> Color(0xFF9C27B0)
                        isNoShow -> Color(0xFFFF9800)
                        else -> Color(0xFFE53935)
                    }

                    Card(
                        Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = bgColor),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column {
                            // Main Content
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp)
                                    .clickable {
                                        navController.navigate("reservation_detail/${res.id}")
                                    },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Icon
                                Icon(
                                    imageVector = when {
                                        isCompleted -> Icons.Default.CheckCircle
                                        isNoShow -> Icons.Default.EventBusy
                                        else -> Icons.Default.Cancel
                                    },
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(32.dp)
                                )

                                Spacer(Modifier.width(16.dp))

                                // Details
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        res.facilityName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = Color.Black
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        res.getDateTimeString(),
                                        fontSize = 13.sp,
                                        color = Color.DarkGray
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Row {
                                        Text(
                                            "Court ${res.courtNumbers.joinToString(",")}",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                        Text(" • ", fontSize = 12.sp, color = Color.Gray)
                                        Text(
                                            "${res.pax} pax",
                                            fontSize = 12.sp,
                                            color = Color.Gray
                                        )
                                    }
                                }

                                // Status Badge
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = statusColor.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        when {
                                            isCompleted -> "Completed"
                                            isNoShow -> "No Show"
                                            else -> "Cancelled"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = statusColor,
                                        modifier = Modifier.padding(
                                            horizontal = 10.dp,
                                            vertical = 5.dp
                                        )
                                    )
                                }
                            }

                            // Rebook Button (for Completed and No Show)
                            if (isCompleted || isNoShow) {
                                HorizontalDivider(
                                    thickness = 1.dp,
                                    color = Color.LightGray.copy(alpha = 0.3f)
                                )

                                TextButton(
                                    onClick = {
                                        navController.navigate("teacher_reserve/${res.facilityId}")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        "Rebook",
                                        color = Color(0xFF5D9CEC),
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}