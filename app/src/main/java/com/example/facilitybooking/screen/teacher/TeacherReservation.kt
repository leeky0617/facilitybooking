package com.example.facilitybooking

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import java.time.LocalDate
import java.time.YearMonth
import java.util.*

// Define static time slots for the UI
val STATIC_TIME_SLOTS = listOf(
    TimeSlot("07:00 AM", "09:00 AM"),
    TimeSlot("09:00 AM", "11:00 AM"),
    TimeSlot("11:00 AM", "01:00 PM"),
    TimeSlot("01:00 PM", "03:00 PM"),
    TimeSlot("03:00 PM", "05:00 PM"),
    TimeSlot("05:00 PM", "07:00 PM")
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TeacherReservationScreen(navController: NavController, facilityId: String,oldResId:String?=null) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()

    // --- STATE VARIABLES ---
    var facility by remember { mutableStateOf<Facility?>(null) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var selectedTimeSlot by remember { mutableStateOf<TimeSlot?>(null) }

    // Capacity & Pax
    var availableCapacity by remember { mutableStateOf(0) }
    var pax by remember { mutableStateOf(1) }

    // UI States
    var showSuccessDialog by remember { mutableStateOf(false) }
    var newReservationId by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var isCheckingAvailability by remember { mutableStateOf(false) }
    // Calendar States
    var currentMonth by remember { mutableStateOf(YearMonth.now()) }
    var showMonthPicker by remember { mutableStateOf(false) }
    var showYearPicker by remember { mutableStateOf(false) }

    val isEditMode = oldResId != null // FIX: Track if we're editing

    LaunchedEffect(facilityId) {
        FirestoreRepository.getFacilityById(facilityId) { fetchedFacility ->
            facility = fetchedFacility
        }
    }

    LaunchedEffect(oldResId) {
        if (oldResId != null) {
            // Load existing reservation
            FirestoreRepository.getReservationById(oldResId) { existingRes ->
                if (existingRes != null) {
                    selectedDate = LocalDate.parse(existingRes.date)
                    selectedTimeSlot = existingRes.timeSlot
                    pax = existingRes.pax
                }
            }
        }
    }

    // --- 2. CHECK AVAILABILITY WHEN DATE/TIME CHANGES ---
    LaunchedEffect(selectedDate, selectedTimeSlot) {
        if (facility != null && selectedDate != null && selectedTimeSlot != null) {
            // Start Loading
            isCheckingAvailability = true

            FirestoreRepository.checkAvailability(
                facilityId = facilityId,
                date = selectedDate.toString(),
                timeSlot = selectedTimeSlot!!,
                totalCapacity = facility!!.totalCapacity
            ) { count ->
                availableCapacity = count
                // Reset pax if it exceeds new capacity
                if (pax > count) pax = 1

                // Stop Loading
                isCheckingAvailability = false
            }
        }
    }

    // Show Loading if facility data isn't ready
    if (facility == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // --- MAIN UI ---
    Box(modifier = Modifier.fillMaxSize().background(Color(0xFFF5F5F5))) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp)
        ) {
            item {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("New Booking", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, "Close")
                    }
                }

                // --- CARD 1: DATE SELECTION ---
                if (!isEditMode) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Facility: ${facility!!.name}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Select a date:", fontSize = 14.sp, color = Color.Gray)
                            Spacer(modifier = Modifier.height(8.dp))

                            // Month/Year Dropdowns
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Month Dropdown
                                Box {
                                    Row(
                                        modifier = Modifier.clickable { showMonthPicker = true },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            currentMonth.month.name.take(3).uppercase(),
                                            fontSize = 16.sp
                                        )
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showMonthPicker,
                                        onDismissRequest = { showMonthPicker = false }) {
                                        (1..12).forEach { month ->
                                            DropdownMenuItem(
                                                text = {
                                                    Text(
                                                        YearMonth.of(
                                                            currentMonth.year,
                                                            month
                                                        ).month.name.take(3)
                                                    )
                                                },
                                                onClick = {
                                                    currentMonth = YearMonth.of(
                                                        currentMonth.year,
                                                        month
                                                    ); showMonthPicker = false
                                                }
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                                // Year Dropdown
                                Box {
                                    Row(
                                        modifier = Modifier.clickable { showYearPicker = true },
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("${currentMonth.year}", fontSize = 16.sp)
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            null,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = showYearPicker,
                                        onDismissRequest = { showYearPicker = false }) {
                                        val currentYear = LocalDate.now().year
                                        listOf(currentYear, currentYear + 1).forEach { year ->
                                            DropdownMenuItem(
                                                text = { Text("$year") },
                                                onClick = {
                                                    currentMonth = YearMonth.of(
                                                        year,
                                                        currentMonth.monthValue
                                                    ); showYearPicker = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Calendar Grid
                            CalendarView(currentMonth, selectedDate) { date ->
                                selectedDate = date
                                selectedTimeSlot = null
                                availableCapacity = 0// Reset time slot when date changes
                            }
                        }
                    }
                } else {
                    // Show locked date
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Editing Reservation",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Date: ${selectedDate}",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "Time: ${selectedTimeSlot?.toString()}",
                                fontSize = 14.sp,
                                color = Color.Gray,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "You can only adjust the number of people (PAX)",
                                fontSize = 12.sp,
                                color = Color(0xFFE53935),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- CARD 2: TIME SLOTS ---
                if (!isEditMode) {
                    Card(
                        Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White)
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Select a period:", fontSize = 14.sp, color = Color.Gray)
                            Spacer(Modifier.height(8.dp))

                            // Filter Logic
                            val validSlots = STATIC_TIME_SLOTS.filter {
                                TimeUtils.isSlotWithinOpeningHours(it, facility!!.openingHours)
                            }

                            if (validSlots.isEmpty()) {
                                Text(
                                    "No slots available within opening hours.",
                                    color = Color.Red,
                                    fontSize = 12.sp
                                )
                            }

                            validSlots.forEach { slot ->
                                TimeSlotButton(
                                    slot,
                                    selectedTimeSlot == slot,
                                    selectedDate != null
                                ) {
                                    if (selectedDate != null) {
                                        selectedTimeSlot = slot; isCheckingAvailability = true
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // --- CARD 3: PAX SLIDER ---
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PAX:", fontSize = 14.sp, color = Color.Gray)
                        Spacer(Modifier.height(16.dp))

                        if (selectedTimeSlot == null) {
                            Text("Select a time first", color = Color.Gray)
                        } else if (isCheckingAvailability) {
                            CircularProgressIndicator(Modifier.size(30.dp), strokeWidth = 3.dp)
                        } else {
                            Text("$pax", fontSize = 48.sp, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(16.dp))

                            if (availableCapacity > 0) {
                                Slider(
                                    value = pax.toFloat(),
                                    onValueChange = { pax = it.toInt() },
                                    valueRange = 1f..availableCapacity.toFloat(),
                                    steps = (availableCapacity - 2).coerceAtLeast(0),
                                    colors = SliderDefaults.colors(thumbColor = Color(0xFFA8D4E6), activeTrackColor = Color(0xFFA8D4E6))
                                )
                                // Fix 4: Showing specific capacity for period
                                Text("Available for this slot: $availableCapacity pax", fontSize = 12.sp, color = Color.Gray)
                            } else {
                                Text("Fully Booked", color = Color.Red, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // --- RESERVE BUTTON ---
                Button(
                    onClick = {
                        if (selectedDate != null && selectedTimeSlot != null) {
                            isLoading = true
                            // ... (Court allocation logic same as before) ...
                            val courtsNeeded = (pax + facility!!.capacityPerCourt - 1) / facility!!.capacityPerCourt
                            val allocatedCourts = (1..courtsNeeded.coerceAtMost(facility!!.courtCount)).toList()

                            val reservation = Reservation(
                                id = "RES${UUID.randomUUID().toString().take(8).uppercase()}",
                                userId = auth.currentUser?.uid ?: "guest",
                                facilityId = facility!!.id,
                                facilityName = facility!!.name,
                                date = selectedDate.toString(),
                                timeSlot = selectedTimeSlot!!,
                                pax = pax,
                                courtNumbers = allocatedCourts,
                                status = "PENDING",
                                bookingId = (10000000..99999999).random().toString()
                            )

                            FirestoreRepository.addReservation(
                                reservation = reservation,
                                onSuccess = { resId ->
                                    isLoading = false
                                    newReservationId = resId
                                    if (oldResId != null) FirestoreRepository.updateReservationStatus(oldResId, "CANCELLED") {}
                                    showSuccessDialog = true
                                },
                                onFailure = {
                                    isLoading = false
                                    Toast.makeText(context, "Failed: $it", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    },
                    // STRICT CHECK: Disable if capacity is 0 or pax > available
                    enabled = selectedDate != null && selectedTimeSlot != null && pax <= availableCapacity && availableCapacity > 0 && !isLoading && !isCheckingAvailability,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D9CEC))
                ) {
                    if (isLoading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text(if (isEditMode) "Save Changes" else "Reserve", fontSize = 16.sp)
                }
                Spacer(Modifier.height(32.dp))
            }
        }

        // --- SUCCESS DIALOG ---
        if (showSuccessDialog) {
            Dialog(onDismissRequest = {}) {
                Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                    Column(modifier = Modifier.padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Success", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Reservation made successfully!", textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                showSuccessDialog = false
                                // Navigate to Detail and remove current screen from backstack
                                navController.navigate("reservation_detail/$newReservationId") {
                                    popUpTo("teacher_main")
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D9CEC))
                        ) {
                            Text("View Ticket")
                        }
                    }
                }
            }
        }
    }
}

// --- HELPER COMPONENT: CALENDAR ---

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarView(
    yearMonth: YearMonth,
    selectedDate: LocalDate?,
    onDateSelected: (LocalDate) -> Unit
) {
    val today = LocalDate.now()
    val tomorrow = today.plusDays(1)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfMonth = yearMonth.atDay(1)

    // FIX: Calculate offset for MONDAY start
    // ISO-8601: Mon=1, Tue=2 ... Sun=7
    // We want Mon=0, Tue=1 ... so we subtract 1.
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value - 1

    Column {
        // Headers starting with Monday
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            listOf("Mo", "Tu", "We", "Th", "Fr", "Sa", "Su").forEach {
                Text(it, Modifier.weight(1f), textAlign = TextAlign.Center, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))

        LazyVerticalGrid(columns = GridCells.Fixed(7), modifier = Modifier.height(250.dp)) {
            // Empty slots for start of month
            items(firstDayOfWeek) { Box(Modifier.aspectRatio(1f)) }

            // Days
            items(daysInMonth) { index ->
                val date = yearMonth.atDay(index + 1)
                val isSelectable = !date.isBefore(tomorrow)

                DayCell(
                    day = date.dayOfMonth,
                    isSelected = date == selectedDate,
                    isSelectable = isSelectable,
                    onClick = { onDateSelected(date) }
                )
            }
        }
    }
}

@Composable
fun DayCell(day: Int, isSelected: Boolean, isSelectable: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .background(if (isSelected) Color(0xFF5D9CEC) else Color.Transparent, CircleShape)
            .border(1.dp, if (isSelectable && !isSelected) Color.LightGray else Color.Transparent, CircleShape)
            .clickable(enabled = isSelectable, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "$day",
            color = if (isSelected) Color.White else if (!isSelectable) Color.LightGray else Color.Black
        )
    }
}

@Composable
fun TimeSlotButton(timeSlot: TimeSlot, isSelected: Boolean, isEnabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled, onClick = onClick),
        color = when {
            isSelected -> Color(0xFF5D9CEC)
            !isEnabled -> Color.White.copy(alpha = 0.5f) // Dimmed if disabled
            else -> Color.White
        },
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isSelected) Color(0xFF5D9CEC) else Color.LightGray
        )
    ) {
        Text(
            timeSlot.toString(),
            modifier = Modifier.padding(16.dp),
            textAlign = TextAlign.Center,
            color = if (isSelected) Color.White else if (!isEnabled) Color.LightGray else Color.Black
        )
    }
}