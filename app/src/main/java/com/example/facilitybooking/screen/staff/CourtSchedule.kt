package com.example.facilitybooking

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.UUID

// --- COLORS ---
val StatusMaintenance = Color(0xFFA6C6D9) // Blue
val StatusCompleted = Color(0xFFC6A6D9)   // Purple
val StatusCheckIn = Color(0xFFD9D9A6)     // Yellow (Pending)
val StatusCanceled = Color(0xFFD9A6A6)    // Red
val StatusInUse = Color(0xFFA6D9A6)       // Green
val StatusNoRes = Color(0xFFEEEEEE)       // Gray

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CourtScheduleScreen(
    navController: NavController,
    courtName: String // e.g., "Court 1"
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    // --- STATE ---
    // Extract court number from string "Court 1" -> 1
    val courtNumber = courtName.filter { it.isDigit() }.toIntOrNull() ?: 1

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var reservations by remember { mutableStateOf<List<Reservation>>(emptyList()) }

    // Dialog States
    var selectedReservation by remember { mutableStateOf<Reservation?>(null) }
    var showStatusDialog by remember { mutableStateOf(false) } // For existing bookings
    var showAddMaintenanceDialog by remember { mutableStateOf(false) } // For empty slots

    // Temp state for adding maintenance
    var targetTimeStart by remember { mutableStateOf("") }

    // --- FETCH DATA ---
    fun loadData() {
        val dateString = selectedDate.toString() // YYYY-MM-DD
        FirestoreRepository.getReservationsByDate("", dateString) { list ->
            // Filter: Only for this specific Court Number
            reservations = list.filter {
                it.courtNumbers.contains(courtNumber) && it.status != "CANCELLED"
            }
        }
    }

    LaunchedEffect(selectedDate) {
        loadData()
    }

    // --- UI ---
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(45.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFCDE8F0))
                    .clickable { navController.popBackStack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Title
        val formatter = DateTimeFormatter.ofPattern("EEE, dd MMM yyyy", Locale.ENGLISH)
        Text(selectedDate.format(formatter), fontSize = 14.sp, color = Color.Gray)
        Text("Facilities Management", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text("Badminton Court - $courtName", fontSize = 18.sp, color = Color.DarkGray)

        Spacer(modifier = Modifier.height(20.dp))

        // --- SCHEDULE ROWS ---
        val slots = listOf("07:00", "09:00", "11:00", "13:00", "15:00", "17:00", "19:00")
        val now = LocalTime.now()
        val isToday = selectedDate == LocalDate.now()

        slots.forEach { startTime ->
            // Find existing booking
            val res = reservations.find { it.timeSlot.startTime.startsWith(startTime) }

            // Status Logic
            val statusText = res?.status ?: "No reservation"
            val statusColor = when {
                res?.status == "PENDING" -> StatusCheckIn
                res?.status == "IN_USE" -> StatusInUse
                res?.status == "Completed" -> StatusCompleted
                res?.status == "Maintenance" || (res?.issue?.isNotEmpty() == true) -> StatusMaintenance
                else -> StatusNoRes
            }

            val displayText = if (res?.issue?.isNotEmpty() == true) "Maintenance: ${res.issue}" else statusText

            // --- RED LINE LOGIC ---
            var showRedLine = false
            var redLineProgress = 0f

            if (isToday) {
                // Parse "07:00" to int 7
                val slotStartHour = startTime.split(":")[0].toInt()
                val slotEndHour = slotStartHour + 2 // Assuming 2 hour slots

                val currentHour = now.hour
                val currentMinute = now.minute

                // Check if NOW is inside this slot
                if (currentHour >= slotStartHour && currentHour < slotEndHour) {
                    showRedLine = true
                    // Calculate progress (0.0 to 1.0)
                    val totalMinutesInSlot = 120f // 2 hours
                    val minutesPassed = (currentHour - slotStartHour) * 60 + currentMinute
                    redLineProgress = minutesPassed / totalMinutesInSlot
                }
            }

            ScheduleRow(
                time = startTime,
                status = displayText,
                color = statusColor,
                showRedLine = showRedLine,
                redLineProgress = redLineProgress,
                onClick = {
                    if (res != null) {
                        selectedReservation = res
                        showStatusDialog = true
                    } else {
                        targetTimeStart = startTime
                        showAddMaintenanceDialog = true
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(50.dp))
    }

    // --- DIALOG 1: UPDATE STATUS ---
    if (showStatusDialog && selectedReservation != null) {
        AlertDialog(
            onDismissRequest = { showStatusDialog = false },
            title = { Text("Update Status") },
            text = { Text("Current: ${selectedReservation!!.status}\nChange status?") },
            confirmButton = {
                Column(Modifier.fillMaxWidth()) {
                    if (selectedReservation!!.status != "Maintenance") {
                        Button(
                            onClick = {
                                FirestoreRepository.updateReservationStatus(selectedReservation!!.id, "IN_USE") {
                                    loadData(); showStatusDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusInUse),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Check In", color = Color.Black) }

                        Button(
                            onClick = {
                                FirestoreRepository.updateReservationStatus(selectedReservation!!.id, "Completed") {
                                    loadData(); showStatusDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusCompleted),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Complete", color = Color.Black) }
                    } else {
                        Button(
                            onClick = {
                                FirestoreRepository.updateReservationStatus(selectedReservation!!.id, "Completed") {
                                    loadData(); showStatusDialog = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = StatusCompleted),
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Finish Maintenance", color = Color.Black) }
                    }

                    Button(
                        onClick = {
                            FirestoreRepository.updateReservationStatus(selectedReservation!!.id, "CANCELLED") {
                                loadData(); showStatusDialog = false
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = StatusCanceled),
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Cancel", color = Color.Black) }
                }
            },
            dismissButton = { TextButton(onClick = { showStatusDialog = false }) { Text("Close") } },
            containerColor = Color.White
        )
    }

    // --- DIALOG 2: ADD MAINTENANCE ---
    if (showAddMaintenanceDialog) {
        var issue by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddMaintenanceDialog = false },
            title = { Text("Block for Maintenance") },
            text = {
                Column {
                    Text("Time: $targetTimeStart", fontSize = 14.sp, color = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = issue,
                        onValueChange = { issue = it },
                        label = { Text("Reason (e.g. Repair)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (issue.isNotEmpty()) {
                            val startH = targetTimeStart.split(":")[0].toInt()
                            val endTime = "${(startH + 2).toString().padStart(2,'0')}:00"

                            val maintenance = Reservation(
                                id = "MAINT_${UUID.randomUUID().toString().take(8)}",
                                userId = auth.currentUser?.uid ?: "staff",
                                facilityId = "1",
                                facilityName = "Badminton Court",
                                date = selectedDate.toString(),
                                timeSlot = TimeSlot(targetTimeStart, endTime),
                                pax = 0,
                                courtNumbers = listOf(courtNumber),
                                status = "Maintenance",
                                issue = issue,
                                bookingId = "MAINT"
                            )
                            FirestoreRepository.addReservation(maintenance, { loadData(); showAddMaintenanceDialog = false }, {})
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = StatusMaintenance)
                ) { Text("Block", color = Color.Black) }
            },
            dismissButton = { TextButton(onClick = { showAddMaintenanceDialog = false }) { Text("Cancel") } },
            containerColor = Color.White
        )
    }
}

@Composable
fun ScheduleRow(
    time: String,
    status: String,
    color: Color,
    showRedLine: Boolean = false,
    redLineProgress: Float = 0f,
    onClick: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxWidth().height(60.dp)) {
        Row(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier.width(70.dp).fillMaxHeight(),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(time, fontSize = 16.sp, color = Color.Black)
            }

            Box(modifier = Modifier.width(1.dp).fillMaxHeight().background(Color.LightGray))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable { onClick() }
                    .padding(4.dp)
            ) {
                if (status.isNotEmpty() && status != "No reservation") {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(4.dp))
                            .background(color)
                            .padding(start = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text(status, fontSize = 14.sp, color = Color.Black)
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(start = 12.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Text("No reservation", fontSize = 14.sp, color = Color.LightGray)
                    }
                }
            }
        }

        // --- RED LINE OVERLAY ---
        if (showRedLine) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val yPos = size.height * redLineProgress
                drawLine(
                    color = Color.Red,
                    start = Offset(0f, yPos),
                    end = Offset(size.width, yPos),
                    strokeWidth = 4f
                )
                // Draw circle indicator at the start
                drawCircle(
                    color = Color.Red,
                    radius = 6f,
                    center = Offset(70.dp.toPx(), yPos) // Start drawing after the time column
                )
            }
        }

        // Bottom divider
        Box(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth().height(1.dp).background(Color.LightGray))
    }
}