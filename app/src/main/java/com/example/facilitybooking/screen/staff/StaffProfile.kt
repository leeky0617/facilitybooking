package com.example.facilitybooking

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth

@Composable
fun StaffProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val currentUserEmail = auth.currentUser?.email ?: "unknown@gmail.com"

    Column(Modifier.fillMaxSize().background(Color.White)) {
        Box(Modifier.fillMaxWidth().height(250.dp).background(Color(0xFFB0D6E5))) {
            Box(Modifier.align(Alignment.TopStart).offset(x = 20.dp, y = 50.dp).size(40.dp).clip(CircleShape).background(Color.White).clickable { navController.popBackStack() }.padding(8.dp), contentAlignment = Alignment.Center) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color(0xFF0A5C7F), modifier = Modifier.size(20.dp)) }

            Column(Modifier.fillMaxWidth().align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(100.dp).clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) { Icon(Icons.Default.Person, "Profile", Modifier.size(50.dp), tint = Color(0xFF0A5C7F)) }
                Spacer(Modifier.height(20.dp))
                Text("Staff Member", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0A5C7F))
                Text(currentUserEmail, fontSize = 15.sp, color = Color(0xFF0A5C7F).copy(alpha = 0.8f))
            }
        }
        Box(Modifier.fillMaxWidth().weight(1f).background(Color(0xFFF1F8FB)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Button(onClick = { navController.navigate("edit_profile") }, Modifier.fillMaxWidth(0.8f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF0A5C7F))) { Text("Edit profile") }
                Spacer(Modifier.height(20.dp))
                Button(onClick = { auth.signOut(); Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show(); navController.navigate("staff_login") { popUpTo(0) { inclusive = true } } }, Modifier.fillMaxWidth(0.8f).height(50.dp), colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE19F9F), contentColor = Color.White)) { Text("Log Out") }
            }
        }
    }
}