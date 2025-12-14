package com.example.facilitybooking

import android.graphics.Bitmap
import android.widget.Toast
import androidx.activity.compose.BackHandler
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


@Composable
fun AddFacilityScreen(navController: NavController) {
    val context = LocalContext.current

    // Form State
    var facilityName by remember { mutableStateOf("") }
    var facilityType by remember { mutableStateOf("Indoor") }
    val facilityOptions = listOf("Indoor", "Outdoor", "Education")
    var numberOfCourt by remember { mutableStateOf("") }
    var maxCapacity by remember { mutableStateOf("") }
    var startHour by remember { mutableStateOf("7") }
    var startAmPm by remember { mutableStateOf("AM") }
    var endHour by remember { mutableStateOf("6") }
    var endAmPm by remember { mutableStateOf("PM") }
    var description by remember { mutableStateOf("") }

    // --- IMAGE STATE ---
    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) } // Controls the popup

    var isLoading by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }

    // 1. Camera Launcher
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bitmap ->
        if (bitmap != null) {
            selectedBitmap = bitmap
        }
    }

    // 2. Gallery Launcher
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            // Convert Uri to Bitmap immediately
            val bitmap = ImageUtils.uriToBitmap(context, uri)
            if (bitmap != null) {
                selectedBitmap = bitmap
            } else {
                Toast.makeText(context, "Failed to load image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    BackHandler { showExitDialog = true }

    fun convertTo24Hour(hour: String, amPm: String): Int {
        val h = hour.toInt()
        return when {
            amPm == "AM" && h == 12 -> 0
            amPm == "AM" -> h
            amPm == "PM" && h == 12 -> 12
            else -> h + 12
        }
    }

    fun saveData(isDraft: Boolean) {
        val finalOpeningHours = "$startHour:00 $startAmPm - $endHour:00 $endAmPm"
        isLoading = true

        // Convert Bitmap to Base64 String
        val imageString = if (selectedBitmap != null) {
            ImageUtils.bitmapToString(selectedBitmap!!)
        } else {
            ""
        }

        FirestoreRepository.addNewFacility(
            name = if (facilityName.isEmpty()) "Untitled Draft" else facilityName,
            type = facilityType,
            courtCount = numberOfCourt.toIntOrNull() ?: 0,
            capacity = maxCapacity.toIntOrNull() ?: 0,
            openingHours = finalOpeningHours,
            description = description,
            imageBase64 = imageString, // Pass the String

            onSuccess = {
                isLoading = false
                Toast.makeText(context, if(isDraft) "Draft Saved" else "Added!", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            },
            onFailure = { error ->
                isLoading = false
                Toast.makeText(context, "Error: $error", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Scaffold(
        containerColor = Color.White,
        bottomBar = {
            Button(
                onClick = {
                    // --- 1. BASIC VALIDATION ---
                    if (facilityName.isBlank() || numberOfCourt.isBlank() || maxCapacity.isBlank()) {
                        Toast.makeText(context, "Fill required fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }

                    // --- 2. TIME VALIDATION ---
                    val start24 = convertTo24Hour(startHour, startAmPm)
                    val end24 = convertTo24Hour(endHour, endAmPm)

                    // Rule: Cannot be earlier than 7 AM (07:00)
                    if (start24 < 7) {
                        Toast.makeText(context, "Opening time cannot be earlier than 7:00 AM", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    // Rule: Cannot be later than 7 PM (19:00)
                    // Note: If you mean the facility CLOSES at 7pm, end time must be <= 19.
                    if (end24 > 19) {
                        Toast.makeText(context, "Closing time cannot be later than 7:00 PM", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    // Rule: Start must be before End
                    if (start24 >= end24) {
                        Toast.makeText(context, "Opening time must be before Closing time", Toast.LENGTH_LONG).show()
                        return@Button
                    }

                    // If all checks pass, save
                    saveData(isDraft = false)
                },
                modifier = Modifier.fillMaxWidth().padding(20.dp).height(55.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LightBlueFill),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                else Text("Add", fontSize = 18.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(20.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(45.dp).clip(RoundedCornerShape(12.dp)).background(LightBlueIcon).clickable { showExitDialog = true }, contentAlignment = Alignment.Center) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.Black)
                }
            }
            Spacer(modifier = Modifier.height(20.dp))

            // --- IMAGE PICKER BOX ---
            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(CircleShape)
                    .border(2.dp, LightBlueFill, CircleShape)
                    .clickable { showImageSourceDialog = true }, // Show dialog on click
                contentAlignment = Alignment.Center
            ) {
                if (selectedBitmap != null) {
                    Image(
                        bitmap = selectedBitmap!!.asImageBitmap(),
                        contentDescription = "Selected",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Add Photo", fontSize = 14.sp, color = Color.Black)
                        Icon(Icons.Outlined.CameraAlt, "Camera", tint = Color.Black)
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            // Inputs
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
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
                TimeSelector(startHour, startAmPm, { startHour = it }, { startAmPm = it }, Modifier.weight(1f))
                Text(" - ", fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                TimeSelector(endHour, endAmPm, { endHour = it }, { endAmPm = it }, Modifier.weight(1f))
            }
            Spacer(modifier = Modifier.height(16.dp))
            FacilityInputLabel("Description")
            FacilityInputField(value = description, onValueChange = { description = it }, isSingleLine = false, height = 100.dp)
            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // --- EXIT DIALOG ---
    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { Text("Discard Changes?") },
            text = { Text("Unsaved changes will be lost.") },
            confirmButton = { Button(onClick = { showExitDialog = false; navController.popBackStack() }, colors = ButtonDefaults.buttonColors(containerColor = Color.Red)) { Text("Discard", color = Color.White) } },
            dismissButton = { TextButton(onClick = { showExitDialog = false }) { Text("Cancel", color = Color.Black) } },
            containerColor = Color.White
        )
    }

    // --- IMAGE SOURCE DIALOG (Camera vs Gallery) ---
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Select Image Source") },
            text = { Text("Choose where to get the image from:") },
            confirmButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    cameraLauncher.launch() // Launch Camera
                }) { Text("Camera") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    galleryLauncher.launch("image/*") // Launch Gallery
                }) { Text("Gallery") }
            },
            containerColor = Color.White
        )
    }
}