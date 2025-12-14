package com.example.facilitybooking

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage


// ==========================================
// 1. EDIT FACILITY SCREEN
// ==========================================
@Composable
fun EditFacilityScreen(navController: NavController, facilityId: String) {
    val context = LocalContext.current

    // --- STATE ---
    var originalFacility by remember { mutableStateOf<Facility?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }

    // Form Fields
    var facilityName by remember { mutableStateOf("") }
    var facilityType by remember { mutableStateOf("Indoor") }
    var numberOfCourt by remember { mutableStateOf("") }
    var maxCapacity by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf("") }
    var imageBase64 by remember { mutableStateOf("") }

    // Image Logic
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    // Time Selection State
    var startHour by remember { mutableStateOf("7") }
    var startAmPm by remember { mutableStateOf("AM") }
    var endHour by remember { mutableStateOf("6") }
    var endAmPm by remember { mutableStateOf("PM") }

    val facilityOptions = listOf("Indoor", "Outdoor", "Education")
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) {
        if (it != null) {
            selectedBitmap = it
            imageBase64 = ImageUtils.bitmapToString(it)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bitmap = ImageUtils.uriToBitmap(context, uri)
            if (bitmap != null) {
                selectedBitmap = bitmap
                imageBase64 = ImageUtils.bitmapToString(bitmap)
            }
        }
    }
    // --- FETCH DATA ---
    LaunchedEffect(facilityId) {
        FirestoreRepository.getFacilityById(facilityId) { facility ->
            if (facility != null) {
                originalFacility = facility
                facilityName = facility.name
                facilityType = facility.category
                numberOfCourt = facility.courtCount.toString()
                maxCapacity = facility.totalCapacity.toString()
                description = facility.description
                imageBase64 = facility.imageBase64

                // Parse Time
                try {
                    val parts = facility.openingHours.split(" - ")
                    if (parts.size == 2) {
                        val startParts = parts[0].split(" ")
                        if (startParts.isNotEmpty()) startHour = startParts[0].split(":")[0]
                        if (startParts.size > 1) startAmPm = startParts[1]

                        val endParts = parts[1].split(" ")
                        if (endParts.isNotEmpty()) endHour = endParts[0].split(":")[0]
                        if (endParts.size > 1) endAmPm = endParts[1]
                    }
                } catch (e: Exception) { }

                isLoading = false
            } else {
                Toast.makeText(context, "Error loading facility", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
        }
    }
    fun convertTo24Hour(hour: String, amPm: String): Int {
        val h = hour.toInt()
        return when {
            amPm == "AM" && h == 12 -> 0
            amPm == "AM" -> h
            amPm == "PM" && h == 12 -> 12
            else -> h + 12
        }
    }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            Button(
                onClick = {
                    if (originalFacility != null) {
                        // --- 1. TIME VALIDATION LOGIC ---
                        val start24 = convertTo24Hour(startHour, startAmPm)
                        val end24 = convertTo24Hour(endHour, endAmPm)

                        // Rule: Cannot be earlier than 7 AM
                        if (start24 < 7) {
                            Toast.makeText(context, "Opening time cannot be earlier than 7:00 AM", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        // Rule: Cannot be later than 7 PM (19:00)
                        if (end24 > 19) {
                            Toast.makeText(context, "Closing time cannot be later than 7:00 PM", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        // Rule: Start must be before End
                        if (start24 >= end24) {
                            Toast.makeText(context, "Opening time must be before Closing time", Toast.LENGTH_LONG).show()
                            return@Button
                        }

                        // --- 2. SAVE IF VALID ---
                        isSaving = true
                        val finalOpeningHours = "$startHour:00 $startAmPm - $endHour:00 $endAmPm"

                        val updatedFacility = originalFacility!!.copy(
                            name = facilityName,
                            category = facilityType,
                            courtCount = numberOfCourt.toIntOrNull() ?: 1,
                            totalCapacity = maxCapacity.toIntOrNull() ?: 10,
                            capacityPerCourt = (maxCapacity.toIntOrNull() ?: 10) / (numberOfCourt.toIntOrNull() ?: 1).coerceAtLeast(1),
                            openingHours = finalOpeningHours,
                            description = description,
                            imageBase64 = imageBase64
                        )

                        FirestoreRepository.updateFacility(
                            facility = updatedFacility,
                            onSuccess = {
                                isSaving = false
                                Toast.makeText(context, "Facility Updated!", Toast.LENGTH_SHORT).show()
                                navController.popBackStack()
                            },
                            onFailure = {
                                isSaving = false
                                Toast.makeText(context, "Error: $it", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(20.dp).height(55.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LightBlueFill),
                enabled = !isLoading && !isSaving
            ) {
                if (isSaving) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                else Text("Update", fontSize = 18.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(20.dp))

                // --- HEADER ---
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(45.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(LightBlueIcon)
                            .clickable { navController.popBackStack() },
                        contentAlignment = Alignment.Center
                    ) {
                        // FIX: Added Icon back
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                    }
                    Spacer(Modifier.width(16.dp))
                    Text("Edit Facility", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- PHOTO PREVIEW ---
                Box(
                    modifier = Modifier.size(140.dp).clip(CircleShape).border(2.dp, LightBlueFill, CircleShape).background(Color.White)
                        .clickable { showImageSourceDialog = true }, // Click triggers dialog
                    contentAlignment = Alignment.Center
                ) {
                    val bitmap = remember(imageBase64, selectedBitmap) {
                        selectedBitmap ?: if(imageBase64.isNotEmpty()) ImageUtils.stringToBitmap(imageBase64) else null
                    }

                    if (bitmap != null) {
                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Preview", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Edit Photo", fontSize = 14.sp, color = Color.Black)
                            Icon(Icons.Outlined.CameraAlt, "Camera", tint = Color.Black)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // --- FORM FIELDS ---
                FacilityInputLabel("Facility Name")
                FacilityInputField(value = facilityName, onValueChange = { facilityName = it })

                Spacer(modifier = Modifier.height(16.dp))
                FacilityInputLabel("Facility Type")
                DropdownSelector(selectedValue = facilityType, options = facilityOptions, onValueChange = { facilityType = it })

                Spacer(modifier = Modifier.height(16.dp))
                FacilityInputLabel("No of court")
                FacilityInputField(value = numberOfCourt, onValueChange = { if (it.all { char -> char.isDigit() }) numberOfCourt = it }, keyboardType = KeyboardType.Number)

                Spacer(modifier = Modifier.height(16.dp))
                FacilityInputLabel("Maximum Capacity")
                FacilityInputField(value = maxCapacity, onValueChange = { if (it.all { char -> char.isDigit() }) maxCapacity = it }, keyboardType = KeyboardType.Number)

                Spacer(modifier = Modifier.height(16.dp))
                FacilityInputLabel("Opening Hour")
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    TimeSelector(hour = startHour, amPm = startAmPm, onHourChange = { startHour = it }, onAmPmChange = { startAmPm = it }, modifier = Modifier.weight(1f))
                    Text(" - ", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                    TimeSelector(hour = endHour, amPm = endAmPm, onHourChange = { endHour = it }, onAmPmChange = { endAmPm = it }, modifier = Modifier.weight(1f))
                }

                Spacer(modifier = Modifier.height(16.dp))
                FacilityInputLabel("Description")
                FacilityInputField(value = description, onValueChange = { description = it }, isSingleLine = false, height = 100.dp)

                Spacer(modifier = Modifier.height(100.dp))
            }
        }
        if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Change Photo") },
            text = { Text("Select source:") },
            confirmButton = { TextButton(onClick = { showImageSourceDialog = false; cameraLauncher.launch() }) { Text("Camera") } },
            dismissButton = { TextButton(onClick = { showImageSourceDialog = false; galleryLauncher.launch("image/*") }) { Text("Gallery") } },
            containerColor = Color.White
        )
    }
    }
}

// ==========================================
// 2. MAINTENANCE EDIT SCREEN
// ==========================================
@Composable
fun MaintenanceEditScreen(
    navController: NavController,
    reservationId: String
) {
    var isLoading by remember { mutableStateOf(true) }
    var issue by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var facilityName by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }

    LaunchedEffect(reservationId) {
        FirestoreRepository.getReservationById(reservationId) { res ->
            if (res != null) {
                facilityName = res.facilityName
                date = res.date
                issue = res.issue
                status = res.status
                isLoading = false
            }
        }
    }

    if (isLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    Column(
        modifier = Modifier.fillMaxSize().background(Color.White).padding(20.dp).verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier.size(45.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFFCDE8F0)).clickable { navController.popBackStack() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Black)
        }

        Spacer(Modifier.height(16.dp))
        Text("Edit Maintenance", fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Text("$facilityName - $date", fontSize = 14.sp, color = Color.Gray)

        Spacer(Modifier.height(25.dp))

        // Uses private helper
        SimpleEditInput("Issue / Task:", issue) { issue = it }
        SimpleEditInput("Status (Pending/Completed):", status) { status = it }

        Spacer(Modifier.height(30.dp))

        Button(
            onClick = {
                FirestoreRepository.updateMaintenanceDetails(reservationId, issue, status) {
                    navController.popBackStack()
                }
            },
            modifier = Modifier.fillMaxWidth().height(55.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCDE8F0)),
            shape = RoundedCornerShape(10.dp)
        ) {
            Text("Save Changes", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SimpleEditInput(label: String, value: String, onValueChange: (String) -> Unit) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(label, fontSize = 14.sp, color = Color.Black, modifier = Modifier.padding(bottom = 8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color(0xFFF0F0F0),
                unfocusedContainerColor = Color(0xFFF0F0F0)
            )
        )
    }
}