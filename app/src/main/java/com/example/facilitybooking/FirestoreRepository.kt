package com.example.facilitybooking

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.auth.User
import kotlinx.coroutines.tasks.await
import com.google.firebase.auth.EmailAuthProvider
import android.graphics.Bitmap

object FirestoreRepository {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // --- FETCH FACILITIES ---
    fun getFacilities(onResult: (List<Facility>) -> Unit) {
        db.collection("facilities").get()
            .addOnSuccessListener { result ->
                val list = result.toObjects(Facility::class.java)
                onResult(list)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    // --- GET SINGLE FACILITY ---
    fun getFacilityById(id: String, onResult: (Facility?) -> Unit) {
        db.collection("facilities").document(id).get()
            .addOnSuccessListener { doc -> onResult(doc.toObject(Facility::class.java)) }
            .addOnFailureListener { onResult(null) }
    }

    // --- ADD RESERVATION ---
    fun addReservation(
        reservation: Reservation,
        onSuccess: (String) -> Unit,
        onFailure: (String) -> Unit
    ) {
        db.collection("reservations").document(reservation.id).set(reservation)
            .addOnSuccessListener { onSuccess(reservation.id) }
            .addOnFailureListener { onFailure(it.message ?: "Error") }
    }

    // --- GET USER RESERVATIONS ---
    fun getUserReservations(onResult: (List<Reservation>) -> Unit) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("reservations").whereEqualTo("userId", userId).get()
            .addOnSuccessListener { result ->
                val list = result.toObjects(Reservation::class.java)
                onResult(list)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    // --- GET SINGLE RESERVATION ---
    fun getReservationById(id: String, onResult: (Reservation?) -> Unit) {
        db.collection("reservations").document(id).get()
            .addOnSuccessListener { doc -> onResult(doc.toObject(Reservation::class.java)) }
            .addOnFailureListener { onResult(null) }
    }

    // --- CHECK AVAILABILITY ---
    // Note: In a real app, do this on a server. Here we fetch all relevant bookings.
    fun checkAvailability(
        facilityId: String,
        date: String,
        timeSlot: TimeSlot,
        totalCapacity: Int,
        onResult: (Int) -> Unit
    ) {
        db.collection("reservations")
            .whereEqualTo("facilityId", facilityId)
            .whereEqualTo("date", date)
            .whereEqualTo("status", "PENDING") // Or COMPLETE
            .get()
            .addOnSuccessListener { result ->
                val reservations = result.toObjects(Reservation::class.java)
                // Filter time slot manually because Firestore query limitations
                val bookedPax = reservations.filter { it.timeSlot == timeSlot }.sumOf { it.pax }
                onResult(totalCapacity - bookedPax)
            }
            .addOnFailureListener { onResult(0) }
    }

    // --- CANCEL RESERVATION ---
    fun updateReservationStatus(id: String, status: String, onSuccess: () -> Unit) {
        db.collection("reservations").document(id).update("status", status)
            .addOnSuccessListener { onSuccess() }
    }

    // --- SEED DATA (RUN ONCE IN MAIN ACTIVITY) ---


    fun updateMaintenanceDetails(
        reservationId: String,
        issue: String,
        status: String,
        onSuccess: () -> Unit
    ) {
        val updates = mapOf(
            "issue" to issue,
            "status" to status
        )
        db.collection("reservations").document(reservationId)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
    }

    // --- NEW: Fetch Reservations by Facility & Date (For Timetable) ---
    fun getReservationsByDate(facilityId: String, date: String, onResult: (List<Reservation>) -> Unit) {
        // In a real app, we filter by facilityId AND date.
        // For this demo, we fetch all and filter locally to ensure it works easily.
        db.collection("reservations")
            .get()
            .addOnSuccessListener { result ->
                val list = result.toObjects(Reservation::class.java)
                val filtered = list.filter {
                    (facilityId.isEmpty() || it.facilityId == facilityId) && // Handle "All" or Specific
                            it.date == date
                }
                onResult(filtered)
            }
            .addOnFailureListener { onResult(emptyList()) }
    }

    fun addNewFacility(
        name: String, type: String, courtCount: Int, capacity: Int, openingHours: String, description: String,
        imageBase64: String, // Accepts String
        onSuccess: () -> Unit, onFailure: (String) -> Unit
    ) {
        val newId = "FAC_${System.currentTimeMillis()}"
        val newFacility = Facility(
            id = newId, name = name, type = type, category = type,
            imageBase64 = imageBase64, // Saves String
            description = description, totalCapacity = capacity, courtCount = courtCount,
            capacityPerCourt = if (courtCount > 0) capacity / courtCount else 0,
            openingHours = openingHours
        )
        db.collection("facilities").document(newId).set(newFacility)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Error") }
    }

    fun deleteFacility(facilityId: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        db.collection("facilities").document(facilityId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Unknown Error") }
    }

    fun updateFacility(
        facility: Facility,

        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {// If there is a new bitmap, convert it. Otherwise keep old string.

        db.collection("facilities").document(facility.id)
            .set(facility) // .set() overwrites the document with the same ID
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Update Failed") }
    }

    fun getUserById(userId: String, onResult: (FacilityUser?) -> Unit) {
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (doc.exists()) {
                    val user = FacilityUser(
                        id = doc.id,
                        name = doc.getString("name") ?: "Unknown",
                        email = doc.getString("email") ?: "",
                        phone = doc.getString("phone") ?: ""
                    )
                    onResult(user)
                } else {
                    onResult(null)
                }
            }
            .addOnFailureListener { onResult(null) }
    }

    fun updateUserProfile(
        userId: String,
        name: String,
        phone: String,
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit
    ) {
        val updates = mapOf(
            "name" to name,
            "phone" to phone
        )
        db.collection("users").document(userId)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it.message ?: "Update Failed") }
    }

    fun changePassword(currentPass: String, newPass: String, onSuccess: () -> Unit, onFailure: (String) -> Unit) {
        val user = auth.currentUser
        if (user != null && user.email != null) {
            // 1. Create Credential with Current Password
            val credential = EmailAuthProvider.getCredential(user.email!!, currentPass)

            // 2. Re-authenticate
            user.reauthenticate(credential)
                .addOnSuccessListener {
                    // 3. Update Password
                    user.updatePassword(newPass)
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onFailure(it.message ?: "Failed to update password") }
                }
                .addOnFailureListener {
                    onFailure("Current password is incorrect")
                }
        } else {
            onFailure("User not logged in")
        }
    }
}