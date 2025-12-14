package com.example.facilitybooking

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.LocalTime
import java.util.UUID

// Colors
val GridMaintenance = Color(0xFFA6C6D9)
val GridCompleted   = Color(0xFFC6A6D9)
val GridCheckIn     = Color(0xFFD9D9A6)
val GridInUsed      = Color(0xFFA6D9A6)
val GridNoRes       = Color(0xFFD3D3D3)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimetableScreen(
    navController: NavController
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    // --- STATE ---
    val today = LocalDate.now()
    var selectedMonth by remember { mutableStateOf(today.monthValue.toString().padStart(2, '0')) }
    var selectedDay by remember { mutableStateOf(today.dayOfMonth.toString().padStart(2, '0')) }
    var selectedYear by remember { mutableStateOf(today.year.toString()) }

    var reservations by remember { mutableStateOf<List<Reservation>>(emptyList()) }

    // Dialog States
    var selectedReservation by remember { mutableStateOf<Reservation?>(null) }
    var showMaintenanceDialog by remember { mutableStateOf(false) }
    var showReservationDialog by remember { mutableStateOf(false) }

    // Add Maintenance State
    var showAddMaintenanceDialog by remember { mutableStateOf(false) }
    var targetCourt by remember { mutableStateOf(0) }
    var targetTimeStart by remember { mutableStateOf("") }

    // FETCH DATA
    val formattedDate = "$selectedYear-$selectedMonth-$selectedDay"
    val isToday = formattedDate == today.toString()

    fun loadData() {
        FirestoreRepository.getReservationsByDate("", formattedDate) { list ->
            // Filter out cancelled items so they don't show on grid
            reservations = list.filter { it.status != "CANCELLED" }
        }
    }

    LaunchedEffect(formattedDate) {
        loadData()
    }

    // --- DIALOGS ---

    // 1. View/Edit Maintenance
    if (showMaintenanceDialog && selectedReservation != null) {
        MaintenanceDialog(
            data = selectedReservation!!.toMaintenanceData(),
            onDismiss = { showMaintenanceDialog = false },
            onEdit = {
                showMaintenanceDialog = false
                navController.navigate("maintenance_edit/${selectedReservation!!.id}")
            },
            onComplete = {
                FirestoreRepository.updateReservationStatus(selectedReservation!!.id, "Completed") {
                    loadData()
                }
            },
            onCancel = {
                FirestoreRepository.updateReservationStatus(selectedReservation!!.id, "CANCELLED") {
                    loadData()
                }
            }
        )
    }

    // 2. View Booking Details
    if (showReservationDialog && selectedReservation != null) {
        ReservationDialog(
            reservation = selectedReservation!!,
            onDismiss = { showReservationDialog = false }
        )
    }

    // 3. Add Maintenance (Block Slot)
    if (showAddMaintenanceDialog) {
        AddMaintenanceDialog(
            court = "Court $targetCourt",
            time = targetTimeStart,
            onDismiss = { showAddMaintenanceDialog = false },
            onConfirm = { issue ->
                val startH = targetTimeStart.split(":")[0].toInt()
                val endTime = "${(startH + 2).toString().padStart(2,'0')}:00"

                val maintenance = Reservation(
                    id = "MAINT_${UUID.randomUUID().toString().take(8)}",
                    userId = auth.currentUser?.uid ?: "staff",
                    facilityId = "1", // In real app, map court number to facility ID
                    facilityName = "Badminton Court",
                    date = formattedDate,
                    timeSlot = TimeSlot(targetTimeStart, endTime),
                    pax = 0,
                    courtNumbers = listOf(targetCourt),
                    status = "Maintenance",
                    issue = issue,
                    bookingId = "MAINT"
                )

                FirestoreRepository.addReservation(maintenance,
                    onSuccess = {
                        Toast.makeText(context, "Slot Blocked", Toast.LENGTH_SHORT).show()
                        showAddMaintenanceDialog = false
                        loadData()
                    },
                    onFailure = { Toast.makeText(context, "Error: $it", Toast.LENGTH_SHORT).show() }
                )
            }
        )
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

        Spacer(modifier = Modifier.height(12.dp))
        Text("Date: $formattedDate", fontSize = 12.sp, color = Color.Gray)
        Text("Facilities Management", fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(20.dp))

        // Date Picker Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFF2F8FA), RoundedCornerShape(8.dp))
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            SimpleGridDropdown(value = selectedMonth, options = (1..12).map { it.toString().padStart(2,'0') }) { selectedMonth = it }
            Spacer(modifier = Modifier.width(16.dp))
            SimpleGridDropdown(value = selectedDay, options = (1..31).map { it.toString().padStart(2,'0') }) { selectedDay = it }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // GRID
        TimetableGrid(
            reservations = reservations,
            showRedLine = isToday,
            onBlockClick = { res ->
                selectedReservation = res
                if (res.issue.isNotEmpty() || res.status == "Maintenance") {
                    showMaintenanceDialog = true
                } else {
                    showReservationDialog = true
                }
            },
            onEmptySlotClick = { timeStart, courtNum ->
                targetCourt = courtNum
                targetTimeStart = timeStart
                showAddMaintenanceDialog = true
            }
        )

        Spacer(modifier = Modifier.height(30.dp))
        LegendSection()
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TimetableGrid(
    reservations: List<Reservation>,
    showRedLine: Boolean,
    onBlockClick: (Reservation) -> Unit,
    onEmptySlotClick: (String, Int) -> Unit
) {
    val times = listOf("07:00", "09:00", "11:00", "13:00", "15:00", "17:00", "19:00")
    val courts = listOf(1, 2, 3, 4, 5, 6)
    val rowHeight = 50.dp

    Box(modifier = Modifier.fillMaxWidth()) {
        Row {
            // Time Column
            Column(modifier = Modifier.width(50.dp)) {
                Spacer(modifier = Modifier.height(30.dp))
                times.forEach { time ->
                    Text(time, fontSize = 14.sp, modifier = Modifier.height(rowHeight).wrapContentHeight(Alignment.CenterVertically))
                }
            }

            // Grid Content
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    courts.forEach { c ->
                        Text("C$c", modifier = Modifier.weight(1f), textAlign = TextAlign.Center, color = Color.Gray, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                Box {
                    Column {
                        times.forEach { timeStart ->
                            Row {
                                courts.forEach { courtNum ->
                                    val match = reservations.find {
                                        it.timeSlot.startTime.startsWith(timeStart) && it.courtNumbers.contains(courtNum)
                                    }

                                    val bg = when {
                                        match == null -> Color.Transparent
                                        match.status == "Completed" -> GridCompleted
                                        match.status == "Maintenance" || match.issue.isNotEmpty() -> GridMaintenance
                                        match.status == "PENDING" -> GridCheckIn
                                        else -> GridInUsed
                                    }

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(rowHeight)
                                            .border(0.5.dp, Color(0xFFEEEEEE))
                                            .padding(2.dp)
                                            .clickable {
                                                if (match != null) {
                                                    onBlockClick(match)
                                                } else {
                                                    onEmptySlotClick(timeStart, courtNum)
                                                }
                                            }
                                    ) {
                                        if (match != null) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(bg)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // --- RED LINE (CURRENT TIME) ---
                    if (showRedLine) {
                        val now = LocalTime.now()
                        if (now.hour in 7..19) {
                            val hoursPassed = (now.hour - 7) + (now.minute / 60f)
                            val yOffset = (hoursPassed / 2 * 50).dp
                            Canvas(modifier = Modifier.fillMaxWidth().offset(y = yOffset)) {
                                drawLine(Color.Red, Offset(0f, 0f), Offset(size.width, 0f), 4f)
                                drawCircle(Color.Red, 12f, Offset(0f, 0f))
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- ADD MAINTENANCE DIALOG ---
@Composable
fun AddMaintenanceDialog(court: String, time: String, onDismiss: () -> Unit, onConfirm: (String) -> Unit) {
    var issue by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Block for Maintenance") },
        text = {
            Column {
                Text("$court @ $time", fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = issue,
                    onValueChange = { issue = it },
                    label = { Text("Reason (e.g. Broken Net)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = { Button(onClick = { if(issue.isNotEmpty()) onConfirm(issue) }, colors = ButtonDefaults.buttonColors(containerColor = GridMaintenance)) { Text("Block", color = Color.Black) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        containerColor = Color.White
    )
}

@Composable fun SimpleGridDropdown(value: String, options: List<String>, onValueChange: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        Row(Modifier.clickable { expanded = true }, verticalAlignment = Alignment.CenterVertically) { Text(value); Icon(Icons.Default.ArrowDropDown,"") }
        DropdownMenu(expanded, { expanded = false }) { options.forEach { DropdownMenuItem({ Text(it) }, { onValueChange(it); expanded = false }) } }
    }
}
@Composable
fun LegendSection() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 16.dp) // Add padding around the whole section
    ) {
        // --- Row 1 ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Use weights to ensure equal spacing (3 columns)
            Box(modifier = Modifier.weight(1f)) {
                LegendItem("Check in", GridCheckIn)
            }
            Box(modifier = Modifier.weight(1f)) {
                LegendItem("In used", GridInUsed)
            }
            Box(modifier = Modifier.weight(1f)) {
                LegendItem("Maintenance", GridMaintenance)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // --- Row 2 ---
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(modifier = Modifier.weight(1f)) {
                LegendItem("No reservation", GridNoRes)
            }
            Box(modifier = Modifier.weight(1f)) {
                LegendItem("Completed", GridCompleted) // Fixed typo "Compleed"
            }
            Box(modifier = Modifier.weight(1f)) {
                LegendItem("Missing", Color(0xFFD9A6A6))
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 4.dp) // Little spacing at the end
    ) {
        // Made the box slightly larger (12dp -> 14dp) and circular for a cleaner look
        Box(
            modifier = Modifier
                .size(14.dp)
                .clip(CircleShape) // Changed to Circle (or keep RoundedCornerShape(4.dp) if you prefer squares)
                .background(color)
        )

        Spacer(modifier = Modifier.width(8.dp)) // Increased spacing between color and text

        Text(
            text = label,
            fontSize = 12.sp, // Slightly larger font for readability
            color = Color.DarkGray,
            fontWeight = FontWeight.Medium,
            maxLines = 1 // Ensure text doesn't wrap weirdly
        )
    }
}