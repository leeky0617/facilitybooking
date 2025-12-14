package com.example.facilitybooking

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun ChangePasswordDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var currentPass by remember { mutableStateOf("") }
    var newPass by remember { mutableStateOf("") }
    var confirmPass by remember { mutableStateOf("") }
    var errorMsg by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text("Change Password", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                Spacer(Modifier.height(16.dp))

                // 1. Current Password
                PasswordField(
                    value = currentPass,
                    onValueChange = { currentPass = it },
                    label = "Current Password"
                )

                Spacer(Modifier.height(8.dp))

                // 2. New Password
                PasswordField(
                    value = newPass,
                    onValueChange = { newPass = it },
                    label = "New Password"
                )

                Spacer(Modifier.height(8.dp))

                // 3. Confirm Password
                PasswordField(
                    value = confirmPass,
                    onValueChange = { confirmPass = it },
                    label = "Confirm New Password"
                )

                if (errorMsg.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Text(errorMsg, color = Color.Red, fontSize = 12.sp)
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.Gray)
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (currentPass.isEmpty() || newPass.isEmpty()) {
                                errorMsg = "Please fill all fields"
                            } else if (newPass != confirmPass) {
                                errorMsg = "New passwords do not match"
                            } else if (newPass.length < 6) {
                                errorMsg = "Password must be at least 6 chars"
                            } else {
                                onConfirm(currentPass, newPass)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5D9CEC))
                    ) {
                        Text("Update")
                    }
                }
            }
        }
    }
}

// --- HELPER COMPONENT FOR PASSWORD FIELDS WITH EYE ICON ---
@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String
) {
    var isVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        visualTransformation = if (isVisible) VisualTransformation.None else PasswordVisualTransformation(),
        trailingIcon = {
            val image = if (isVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            val description = if (isVisible) "Hide password" else "Show password"

            IconButton(onClick = { isVisible = !isVisible }) {
                Icon(imageVector = image, contentDescription = description)
            }
        }
    )
}