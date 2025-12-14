package com.example.facilitybooking

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

// Colors
private val LightBlueData = Color(0xFFA8D4E6)
private val BorderGrey = Color(0xFFE0E0E0)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeacherSearchScreen(navController: NavController) {
    // --- LOCAL DATA (Replaces MockRepository) ---
    val availableTimeSlots = listOf(
        TimeSlot("07:00", "09:00"),
        TimeSlot("09:00", "11:00"),
        TimeSlot("11:00", "13:00"),
        TimeSlot("13:00", "15:00"),
        TimeSlot("15:00", "17:00")
    )

    // --- STATE ---
    var searchText by remember { mutableStateOf("") }

    // Filters
    var selectedCategory by remember { mutableStateOf<FacilityCategory?>(FacilityCategory.INDOOR) }

    var selectedDay by remember { mutableStateOf("25") }
    var selectedMonth by remember { mutableStateOf("Sep") }
    var selectedYear by remember { mutableStateOf("2025") }

    var selectedTimeSlot by remember { mutableStateOf<TimeSlot?>(availableTimeSlots.firstOrNull()) }
    var capacityValue by remember { mutableStateOf(50f) }

    // Search Results
    var allFacilities by remember { mutableStateOf<List<Facility>>(emptyList()) }
    var searchResults by remember { mutableStateOf<List<Facility>>(emptyList()) }
    var hasSearched by remember { mutableStateOf(false) }

    // Fetch Data from Firestore
    LaunchedEffect(Unit) {
        FirestoreRepository.getFacilities { result ->
            // Just assign the result directly (Draft feature was removed)
            allFacilities = result
        }
    }

    Scaffold(
        containerColor = Color.White
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 20.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // --- 1. TOP BAR ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(45.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(LightBlueData)
                        .clickable { navController.popBackStack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.Black
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    placeholder = { Text("Hinted search text", color = Color.Gray, fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Menu, null, tint = Color.Gray) },
                    trailingIcon = { Icon(Icons.Default.Search, null, tint = Color.Gray) },
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(25.dp)),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = LightBlueData.copy(alpha = 0.5f),
                        unfocusedContainerColor = LightBlueData.copy(alpha = 0.5f),
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    singleLine = true
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
            Text("Filters", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            // Scrollable Content
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(bottom = 20.dp)
            ) {
                // --- 2. FACILITIES TYPE ---
                item {
                    Text("Facilities Type", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FilterButton("Indoor", selectedCategory == FacilityCategory.INDOOR) { selectedCategory = FacilityCategory.INDOOR }
                        FilterButton("Outdoor", selectedCategory == FacilityCategory.OUTDOOR) { selectedCategory = FacilityCategory.OUTDOOR }
                        FilterButton("Education", selectedCategory == FacilityCategory.EDUCATION) { selectedCategory = FacilityCategory.EDUCATION }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // --- 3. DATE ---
                item {
                    Text("Date", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DateDropdownItem(selectedDay, Modifier.weight(0.8f), (1..31).map { it.toString() }) { selectedDay = it }
                        DateDropdownItem(selectedMonth, Modifier.weight(1f), listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")) { selectedMonth = it }
                        DateDropdownItem(selectedYear, Modifier.weight(1f), listOf("2025", "2026")) { selectedYear = it }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // --- 4. TIME SLOTS (Fixed Loop) ---
                item {
                    Text("Time", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))

                    // We use a Column to stack them
                    Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                        // Loop through the local list, NOT MockRepository
                        availableTimeSlots.forEach { slot ->
                            val isSelected = selectedTimeSlot == slot

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(if (isSelected) LightBlueData else Color.White)
                                    .border(1.dp, BorderGrey)
                                    .clickable { selectedTimeSlot = slot }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = slot.toString(),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.Black
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // --- 5. CAPACITY ---
                item {
                    Text("Capacity", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text("${capacityValue.toInt()} Pax", fontSize = 12.sp, color = Color.Gray)
                    Slider(
                        value = capacityValue,
                        onValueChange = { capacityValue = it },
                        valueRange = 0f..200f,
                        colors = SliderDefaults.colors(
                            thumbColor = Color(0xFF006C9C),
                            activeTrackColor = Color(0xFF006C9C),
                            inactiveTrackColor = LightBlueData
                        )
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                }

                // --- 6. APPLY BUTTON ---
                item {
                    Button(
                        onClick = {
                            // Filter Logic
                            searchResults = allFacilities.filter { facility ->
                                val categoryMatch = if (selectedCategory == null) true else
                                    facility.category.equals(selectedCategory!!.name, ignoreCase = true)
                                val capacityMatch = facility.totalCapacity >= capacityValue.toInt()
                                categoryMatch && capacityMatch
                            }
                            hasSearched = true
                            searchResults = emptyList()

                            allFacilities.forEach { fac ->
                                // 1. Category Filter
                                val catMatch = if (selectedCategory == null) true else fac.category.equals(selectedCategory!!.name, ignoreCase = true)
                                // 2. Capacity Filter
                                val capMatch = fac.totalCapacity >= capacityValue.toInt()

                                if (catMatch && capMatch) {
                                    if (selectedTimeSlot != null) {
                                        // 3. Time Filter: Check Firestore Availability
                                        val searchDate = "$selectedYear-$selectedMonth-$selectedDay" // Need proper formatting logic here usually
                                        // For demo, just checking capacity generically or assuming today if date not fully parsed

                                        // Simple logic: If time slot is selected, check if facility supports that time (opening hours)
                                        val timeMatch = TimeUtils.isSlotWithinOpeningHours(selectedTimeSlot!!, fac.openingHours)

                                        if (timeMatch) {
                                            searchResults = searchResults + fac
                                        }
                                    } else {
                                        searchResults = searchResults + fac
                                    }
                                }
                            }

                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = LightBlueData),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text("Apply Filters", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // --- 7. RESULTS ---
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${searchResults.size} results found", fontSize = 14.sp, color = Color.Black)
                        Text(
                            "Clear filters",
                            fontSize = 14.sp,
                            color = Color(0xFF009688),
                            modifier = Modifier.clickable {
                                selectedCategory = null
                                searchResults = emptyList()
                                hasSearched = false
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(40.dp))
                }

                if (hasSearched && searchResults.isEmpty()) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Outlined.Info, null, tint = Color.Gray, modifier = Modifier.size(60.dp))
                            Spacer(modifier = Modifier.height(10.dp))
                            Text("No facility found", color = Color.Gray, fontSize = 14.sp)
                            Text("Try adjusting your search or filters", color = Color.Gray, fontSize = 12.sp)
                        }
                    }
                } else if (hasSearched) {
                    items(searchResults) { facility ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .clickable { navController.navigate("teacher_facility/${facility.id}") },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F5F5))
                        ) {
                            Row(Modifier.padding(16.dp)) {
                                Box(
                                    modifier = Modifier
                                        .size(60.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color.Gray),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val bitmap = remember(facility.imageBase64) {
                                        if (facility.imageBase64.isNotEmpty()) ImageUtils.stringToBitmap(facility.imageBase64) else null
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        Icon(Icons.Outlined.Image, null, tint = Color.White)
                                    }
                                }
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(facility.name, fontWeight = FontWeight.Bold)
                                    Text("Capacity: ${facility.totalCapacity}", fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- COMPONENTS ---

@Composable
fun FilterButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) LightBlueData else Color(0xFFEEEEEE),
        border = if (isSelected) BorderStroke(1.dp, Color(0xFF006C9C)) else null,
        modifier = Modifier.height(35.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 20.dp)) {
            Text(text, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal, color = Color.Black)
        }
    }
}

@Composable
fun DateDropdownItem(text: String, modifier: Modifier = Modifier, options: List<String>, onSelect: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            onClick = { expanded = true },
            shape = RoundedCornerShape(8.dp),
            border = BorderStroke(1.dp, Color.Gray),
            color = Color.White,
            modifier = Modifier.fillMaxWidth().height(40.dp)
        ) {
            Row(Modifier.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text, fontSize = 14.sp)
                Icon(Icons.Default.ArrowDropDown, null, tint = Color.Black)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, modifier = Modifier.background(Color.White).heightIn(max = 200.dp)) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = { onSelect(option); expanded = false })
            }
        }
    }
}