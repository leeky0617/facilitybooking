package com.example.facilitybooking

import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// --- FACILITY ---
data class Facility(
    val id: String = "",
    val name: String = "",
    val type: String = "General",
    val category: String = "INDOOR",
    val imageBase64: String = "",
    val description: String = "",
    val totalCapacity: Int = 0,
    val courtCount: Int = 0,
    val capacityPerCourt: Int = 0,
    val openingHours: String = "7:00 AM - 6:00 PM",


)

enum class FacilityCategory { INDOOR, OUTDOOR, EDUCATION }

// --- RESERVATION ---
data class TimeSlot(val startTime: String = "", val endTime: String = "") {
    override fun toString() = "$startTime - $endTime"
}

data class Reservation(
    val id: String = "",
    val userId: String = "",
    val facilityId: String = "",
    val facilityName: String = "",
    val date: String = "",
    val timeSlot: TimeSlot = TimeSlot(),
    val pax: Int = 0,
    val courtNumbers: List<Int> = emptyList(),
    val status: String = "PENDING",
    val bookingId: String = "",
    // NEW FIELD FOR STAFF MAINTENANCE:
    val issue: String = ""

) {
    @RequiresApi(Build.VERSION_CODES.O)
    fun getDateTimeString(): String {
        return try {
            val parsedDate = LocalDate.parse(date)
            val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH)
            "${parsedDate.format(formatter)}, $timeSlot"
        } catch (e: Exception) {
            "$date, $timeSlot"
        }
    }
}
data class FacilityUser(
    var id: String = "",
    var name: String = "Unknown",
    var email: String = "",
    var phone: String = "",
    var role: String = "Teacher" // Added role for clarity
)
// Helper to convert Reservation to MaintenanceData for the Dialog
fun Reservation.toMaintenanceData(): MaintenanceData {
    return MaintenanceData(
        facility = this.facilityName,
        issue = if (this.issue.isEmpty()) "Routine Check" else this.issue,
        court = "Court ${this.courtNumbers.joinToString(",")}",
        startDate = this.date,
        endDate = this.date,
        time = this.timeSlot.toString(),
        status = this.status
    )
}

// Keep this for UI compatibility
data class MaintenanceData(
    val facility: String,
    val issue: String,
    val court: String,
    val startDate: String,
    val endDate: String,
    val time: String,
    val status: String
)