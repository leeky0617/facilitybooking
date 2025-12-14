package com.example.facilitybooking

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@Composable
fun MaintenanceDialog(
    data: MaintenanceData,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onComplete: () -> Unit,
    onCancel: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Close Icon (Top Right)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        modifier = Modifier.clickable { onDismiss() }
                    )
                }

                Text(
                    text = "Maintenance Schedule",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )

                Spacer(Modifier.height(20.dp))

                // --- DATA DISPLAY ---
                DetailRowText(data.issue)        // e.g. "Water Leakage"
                DetailRowText(data.facility)     // e.g. "Badminton Court"
                DetailRowText(data.court)        // e.g. "Court 6"
                DetailRowText(data.startDate)    // e.g. "2025-12-22"
                DetailRowText(data.time)         // e.g. "07:00 - 09:00"

                Spacer(Modifier.height(10.dp))

                Text("Issued by:", fontSize = 10.sp, color = Color.Gray)
                Text("Staff Member", fontSize = 14.sp) // You can replace this with actual user name if available

                Spacer(Modifier.height(10.dp))

                Text("Status:", fontSize = 10.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))

                // Status Badge
                val statusColor = if (data.status == "Completed") Color(0xFFC6A6D9) else Color(0xFFA6C6D9)

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(statusColor)
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                ) {
                    Text(data.status, fontSize = 12.sp, color = Color.Black)
                }

                Spacer(Modifier.height(25.dp))

                // Action Buttons Row (Edit / Cancel)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Edit Button
                    OutlinedButton(
                        onClick = onEdit,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Edit", color = Color.Black)
                    }

                    Spacer(Modifier.width(16.dp))

                    // Cancel Button
                    OutlinedButton(
                        onClick = {
                            onCancel()
                            onDismiss()
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Cancel", color = Color.Black)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Complete Button
                Button(
                    onClick = {
                        onComplete()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFCDE8F0)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Mark Complete", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Helper for styling the text rows
@Composable
fun DetailRowText(text: String) {
    Text(
        text = text,
        fontSize = 14.sp,
        modifier = Modifier.padding(vertical = 2.dp),
        color = Color.Black
    )
}