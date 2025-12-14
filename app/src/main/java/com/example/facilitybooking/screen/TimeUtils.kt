package com.example.facilitybooking

import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeUtils {
    // Check if a slot (e.g., "09:00 AM - 11:00 AM") fits within opening hours (e.g., "7:00 AM - 6:00 PM")
    fun isSlotWithinOpeningHours(slot: TimeSlot, openingHours: String): Boolean {
        return try {
            val parts = openingHours.split(" - ")
            if (parts.size != 2) return true

            // --- CHANGED: Both Facility and Slots now use "h:mm a" (AM/PM) ---
            // "h" handles single digit hours (7:00), "hh" handles double (07:00), both work with this formatter generally
            // but for safety we use flexible parsing or ensure input format matches.

            // This formatter handles "07:00 AM" and "7:00 AM"
            val timeFormatter = DateTimeFormatter.ofPattern("[h:mm a][hh:mm a]", Locale.ENGLISH)

            // Parse Facility Hours
            val openTime = LocalTime.parse(parts[0].uppercase(), timeFormatter)
            val closeTime = LocalTime.parse(parts[1].uppercase(), timeFormatter)

            // Parse Selected Slot (Now in AM/PM)
            val slotStart = LocalTime.parse(slot.startTime.uppercase(), timeFormatter)
            val slotEnd = LocalTime.parse(slot.endTime.uppercase(), timeFormatter)

            // Logic: Slot must start AFTER Open AND end BEFORE Close
            // ( using !isBefore and !isAfter covers "equal to" cases)
            !slotStart.isBefore(openTime) && !slotEnd.isAfter(closeTime)
        } catch (e: Exception) {
            // If parsing fails (e.g., typo in database), allow the slot to be safe
            true
        }
    }
}