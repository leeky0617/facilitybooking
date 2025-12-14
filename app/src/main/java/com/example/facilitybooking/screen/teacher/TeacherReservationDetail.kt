package com.example.facilitybooking

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import java.time.LocalDate

// Custom Colors
private val HeaderBackground = Color(0xFFF2F8FA)
private val ButtonGreen = Color(0xFFA6D9A6)
private val ButtonRed = Color(0xFFE6A6A6)
private val TextGray = Color(0xFF555555)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TeacherReservationDetailScreen(navController: NavController, reservationId: String) {
    var reservation by remember { mutableStateOf<Reservation?>(null) }
    var showCancelDialog by remember { mutableStateOf(false) }
    var showCancelSuccess by remember { mutableStateOf(false) }

    // Fetch Data
    LaunchedEffect(reservationId) {
        FirestoreRepository.getReservationById(reservationId) { reservation = it }
    }

    if (reservation == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // Determine status and permissions
    val isHistory = reservation!!.status == "Completed" || reservation!!.status == "CANCELLED"
    val isCompleted = reservation!!.status == "Completed"
    val isCancelled = reservation!!.status == "CANCELLED"
    val isActive = reservation!!.status == "PENDING" || reservation!!.status == "IN_USE"
    val isInUse = reservation!!.status == "IN_USE"
    val isPending = reservation!!.status == "PENDING"

    // Date calculations for edit/cancel permissions
    val today = LocalDate.now()
    val bookingDate = try { LocalDate.parse(reservation!!.date) } catch (e: Exception) { today }
    val daysUntilBooking = java.time.temporal.ChronoUnit.DAYS.between(today, bookingDate)
    val canEdit = isPending && daysUntilBooking >= 3 // Can only edit if 3+ days before
    val canCancel = isPending && daysUntilBooking >= 3 // Can only cancel if 3+ days before

    // Generate QR only for active bookings
    val qrBitmap = remember(reservation!!.id, isActive) {
        if (isActive) {
            QRCodeGenerator.generateQRCode("BOOKING:${reservation!!.id}", 512)
        } else null
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .verticalScroll(rememberScrollState())
    ) {

        // --- 1. TOP BAR ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    when {
                        isCompleted -> Color(0xFFF3E5F5)
                        isCancelled -> Color(0xFFFFEBEE)
                        else -> HeaderBackground
                    }
                )
                .statusBarsPadding()
                .padding(vertical = 16.dp)
        ) {
            IconButton(
                onClick = { navController.popBackStack() },
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.Black
                )
            }

            Text(
                text = reservation!!.getDateTimeString(),
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // --- 2. FACILITY NAME HEADER ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    when {
                        isCompleted -> Color(0xFFF3E5F5)
                        isCancelled -> Color(0xFFFFEBEE)
                        else -> HeaderBackground
                    }
                )
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (isHistory) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = when {
                            isCompleted -> Color(0xFF9C27B0)
                            else -> Color(0xFFE53935)
                        }
                    ) {
                        Text(
                            if (isCompleted) "COMPLETED" else "CANCELLED",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Text(
                    text = reservation!!.facilityName,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }

        // --- 3. DETAILS LIST ---
        Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 10.dp)) {
            ReservationDetailRow("Facilities Type", "Indoor")

            val courtText = if (reservation!!.courtNumbers.isNotEmpty())
                "Court ${reservation!!.courtNumbers.joinToString(",")}" else "-"
            ReservationDetailRow("Facilities No", courtText)

            ReservationDetailRow("PAX", reservation!!.pax.toString())

            ReservationDetailRow("Booking ID", reservation!!.bookingId)

            ReservationDetailRow("Status", reservation!!.status)
        }

        Spacer(modifier = Modifier.height(30.dp))

        // --- 4. QR CODE OR INFO BOX ---
        if (isActive && qrBitmap != null) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Present the QR to staff to check in facilities",
                    fontSize = 13.sp,
                    color = Color.Gray,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Image(
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier.size(220.dp)
                )
            }
        } else if (isHistory) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF9E6)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = if (isCompleted) "✓ Booking Completed" else "✕ Booking Cancelled",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF856404)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isCompleted)
                            "This booking has been completed. QR code is no longer available."
                        else
                            "This booking was cancelled. QR code is no longer valid.",
                        fontSize = 13.sp,
                        color = Color(0xFF856404),
                        lineHeight = 18.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // --- 5. ACTION BUTTONS ---
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp)) {
            // IN_USE: No buttons (already checked in)
            if (isInUse) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Currently In Use",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "This booking has been checked in. No edits or cancellations allowed.",
                            fontSize = 13.sp,
                            color = Color.DarkGray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // PENDING: Edit and Cancel buttons (with permission check)
            else if (isPending) {
                // Edit Button
                Button(
                    onClick = {
                        navController.navigate("teacher_reserve/${reservation!!.facilityId}?oldId=$reservationId")
                    },
                    enabled = canEdit,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
                ) {
                    Text(
                        if (canEdit) "Edit PAX" else "Edit Locked (<3 days)",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Cancel Button
                Button(
                    onClick = { showCancelDialog = true },
                    enabled = canCancel,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonRed)
                ) {
                    Text(
                        if (canCancel) "Cancel Reservation" else "Cancellation Locked (<3 days)",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // COMPLETED: Rebook button
            else if (isCompleted) {
                Button(
                    onClick = {
                        navController.navigate("teacher_reserve/${reservation!!.facilityId}")
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ButtonGreen)
                ) {
                    Text(
                        "Rebook This Facility",
                        color = Color.Black,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }

    // --- CANCEL CONFIRMATION DIALOG ---
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text("Cancel Reservation?") },
            text = { Text("Are you sure you want to cancel this booking?") },
            confirmButton = {
                Button(
                    onClick = {
                        FirestoreRepository.updateReservationStatus(reservationId, "CANCELLED") {
                            showCancelDialog = false
                            showCancelSuccess = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) { Text("Yes, Cancel", color = Color.White) }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text("No", color = Color.Black)
                }
            },
            containerColor = Color.White
        )
    }

    // --- CANCEL SUCCESS DIALOG ---
    if (showCancelSuccess) {
        Dialog(onDismissRequest = {}) {
            Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Column(
                    Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Cancelled", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Reservation successfully cancelled.",
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = {
                            showCancelSuccess = false
                            navController.popBackStack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D9CEC))
                    ) { Text("OK") }
                }
            }
        }
    }
}

// --- HELPER COMPONENT FOR ROWS ---
@Composable
fun ReservationDetailRow(label: String, value: String) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 15.sp, color = TextGray)
            Text(text = value, fontSize = 15.sp, color = Color.Black, fontWeight = FontWeight.Medium)
        }
        HorizontalDivider(thickness = 1.dp, color = Color(0xFFEEEEEE))
    }
}