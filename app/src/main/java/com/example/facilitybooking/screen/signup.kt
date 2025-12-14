package com.example.facilitybooking

import android.util.Patterns
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(navController: NavController) {
    val context = LocalContext.current
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    // Default role selection
    var selectedRole by remember { mutableStateOf("Teacher") }
    var isLoading by remember { mutableStateOf(false) }

    fun isValidGmail(email: String): Boolean {
        // Just checks for @ symbol and pattern, no @gmail.com enforcement
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    Scaffold(containerColor = Color.White) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                IconButton(onClick = { navController.popBackStack() }, modifier = Modifier.size(24.dp)) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.Black)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            Text("Sign Up", fontSize = 28.sp, fontWeight = FontWeight.Medium, color = Color.Black)
            Spacer(modifier = Modifier.height(20.dp))
            Image(painter = painterResource(id = R.drawable.school_logo), contentDescription = "Logo", modifier = Modifier.size(150.dp), contentScale = ContentScale.Fit)
            Spacer(modifier = Modifier.height(20.dp))

            // Role Selection
            Text("I am a:", fontSize = 14.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                // Teacher Radio
                RadioButton(selected = (selectedRole == "Teacher"), onClick = { selectedRole = "Teacher" }, colors = RadioButtonDefaults.colors(selectedColor = Color.Black))
                Text(text = "Teacher", modifier = Modifier.clickable { selectedRole = "Teacher" }, color = Color.Black)

                Spacer(modifier = Modifier.width(24.dp))

                // Staff Radio
                RadioButton(selected = (selectedRole == "Staff"), onClick = { selectedRole = "Staff" }, colors = RadioButtonDefaults.colors(selectedColor = Color.Black))
                Text(text = "Staff", modifier = Modifier.clickable { selectedRole = "Staff" }, color = Color.Black)
            }

            Spacer(modifier = Modifier.height(16.dp))

            InputLabel("Full Name")
            CustomTextField(value = name, onValueChange = { name = it }, keyboardType = KeyboardType.Text)
            Spacer(modifier = Modifier.height(16.dp))

            InputLabel("Gmail")
            CustomTextField(value = email, onValueChange = { email = it }, keyboardType = KeyboardType.Email)
            Spacer(modifier = Modifier.height(16.dp))

            InputLabel("Password")
            CustomPasswordField(value = password, onValueChange = { password = it })
            Spacer(modifier = Modifier.height(16.dp))

            InputLabel("Confirm Password")
            CustomPasswordField(value = confirmPassword, onValueChange = { confirmPassword = it })
            Spacer(modifier = Modifier.height(30.dp))

            Button(
                onClick = {
                    if (name.isNotEmpty() && email.isNotEmpty() && password.isNotEmpty()) {
                        if (isValidGmail(email)) {
                            if (password == confirmPassword) {
                                isLoading = true
                                val auth = FirebaseAuth.getInstance()
                                val db = FirebaseFirestore.getInstance()
                                auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val userId = auth.currentUser?.uid

                                        // --- SAVE ROLE TO DATABASE ---
                                        val userData = hashMapOf(
                                            "name" to name,
                                            "email" to email,
                                            "role" to selectedRole // Stores "Teacher" or "Staff"
                                        )

                                        if (userId != null) {
                                            db.collection("users").document(userId)
                                                .set(userData)
                                                .addOnSuccessListener {
                                                    isLoading = false
                                                    auth.currentUser?.sendEmailVerification()
                                                    Toast.makeText(context, "Account Created! Please verify email.", Toast.LENGTH_SHORT).show()

                                                    // Sign out to force login check
                                                    auth.signOut()

                                                    // Navigate based on selection
                                                    if (selectedRole == "Staff") {
                                                        navController.navigate("staff_login") { popUpTo("sign_up") { inclusive = true } }
                                                    } else {
                                                        navController.navigate("teacher_login") { popUpTo("sign_up") { inclusive = true } }
                                                    }
                                                }
                                                .addOnFailureListener {
                                                    isLoading = false
                                                    Toast.makeText(context, "Database Error: ${it.message}", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    } else {
                                        isLoading = false
                                        Toast.makeText(context, "Sign Up Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            } else { Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show() }
                        } else { Toast.makeText(context, "Please enter a valid email", Toast.LENGTH_SHORT).show() }
                    } else { Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show() }
                },
                modifier = Modifier.fillMaxWidth().height(55.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = LightBlueFill),
                enabled = !isLoading
            ) {
                if (isLoading) CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(24.dp))
                else Text("Sign Up", fontSize = 18.sp, color = Color.Black, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}


@Composable
fun InputLabel(text: String) { Column { Text(text = text, fontSize = 12.sp, color = Color.Black); Spacer(modifier = Modifier.height(8.dp)) } }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomTextField(value: String, onValueChange: (String) -> Unit, keyboardType: KeyboardType) {
    OutlinedTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth().height(55.dp), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = InputBg, unfocusedContainerColor = InputBg, disabledContainerColor = InputBg, focusedBorderColor = LightBlueFill, unfocusedBorderColor = LightBlueStroke), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = keyboardType))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPasswordField(value: String, onValueChange: (String) -> Unit) {
    var passwordVisible by remember { mutableStateOf(false) }
    OutlinedTextField(value = value, onValueChange = onValueChange, modifier = Modifier.fillMaxWidth().height(55.dp), shape = RoundedCornerShape(12.dp), colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = InputBg, unfocusedContainerColor = InputBg, disabledContainerColor = InputBg, focusedBorderColor = LightBlueFill, unfocusedBorderColor = LightBlueStroke), visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password), trailingIcon = {
        val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
        IconButton(onClick = { passwordVisible = !passwordVisible }) { Icon(imageVector = image, contentDescription = null) }
    })
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SignUpPreview() {
    val navController = rememberNavController()
    MaterialTheme { SignUpScreen(navController) }
}