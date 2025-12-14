package com.example.facilitybooking

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun LoadingScreen(navController: NavController) {
    // This runs once when the app opens
    LaunchedEffect(Unit) {
        val auth = FirebaseAuth.getInstance()
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // 1. User is logged in, check their Role in Firestore
            val db = FirebaseFirestore.getInstance()

            db.collection("users").document(currentUser.uid).get()
                .addOnSuccessListener { document ->
                    val role = document.getString("role")

                    // 2. Route based on Role
                    if (role == "Teacher") {
                        navController.navigate("teacher_main") {
                            popUpTo("loading") { inclusive = true } // Clear backstack
                        }
                    } else if (role == "Staff") {
                        navController.navigate("facilities") {
                            popUpTo("loading") { inclusive = true }
                        }
                    } else {
                        // Role unknown or missing, send to login
                        navController.navigate("teacher_login") {
                            popUpTo("loading") { inclusive = true }
                        }
                    }
                }
                .addOnFailureListener {
                    // Network error or issue, send to login
                    navController.navigate("teacher_login") {
                        popUpTo("loading") { inclusive = true }
                    }
                }
        } else {
            // 3. No user logged in, go to Login screen
            navController.navigate("teacher_login") {
                popUpTo("loading") { inclusive = true }
            }
        }
    }

    // UI: School Logo centered on white background
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.school_logo),
            contentDescription = "Loading...",
            modifier = Modifier.size(250.dp), // Adjust size if needed
            contentScale = ContentScale.Fit
        )
    }
}