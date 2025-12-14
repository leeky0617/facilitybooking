package com.example.facilitybooking

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            MaterialTheme {
                val navController = rememberNavController()

                // Shared State for Staff Maintenance


                NavHost(navController = navController, startDestination = "loading") {
                    composable("loading") {
                        LoadingScreen(navController)
                    }
                    // --- AUTH ---
                    composable("teacher_login") { TeacherLoginScreen(navController) }
                    composable("staff_login") { StaffLoginScreen(navController) }
                    composable("sign_up") { SignUpScreen(navController) }
                    composable("forgot_password") { ForgotPasswordScreen(navController) }

                    // --- STAFF FLOW ---
                    composable("facilities") { FacilitiesScreen(navController) }
                    composable("staff_profile") { StaffProfileScreen(navController) }
                    composable("edit_profile") { EditProfileScreen(navController) } // Staff Edit
                    composable("qr_scan") { QrScanScreen(navController) }
                    // 1. TIMETABLE
                    composable("timetable") {
                        TimetableScreen(navController)
                    }

                    // 2. MAINTENANCE EDIT (Now accepts ID)
                    composable(
                        route = "maintenance_edit/{resId}",
                        arguments = listOf(navArgument("resId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val resId = backStackEntry.arguments?.getString("resId") ?: ""
                        MaintenanceEditScreen(navController, resId)
                    }
                    composable("edit_facility/{facilityId}") { backStackEntry ->
                        val facilityId = backStackEntry.arguments?.getString("facilityId") ?: ""
                        EditFacilityScreen(navController, facilityId)
                    }
                    composable("facilityDetail/{name}") { FacilityDetailScreen(navController, it.arguments?.getString("name")?:"") }
                    composable("court_schedule/{courtName}") { CourtScheduleScreen(navController, it.arguments?.getString("courtName")?:"") }
                    composable("add_facility") {
                        AddFacilityScreen(navController)
                    }
                    composable("staff_reservation_detail/{reservationId}") { backStackEntry ->
                        val resId = backStackEntry.arguments?.getString("reservationId") ?: ""
                        StaffReservationDetailScreen(navController, resId)
                    }
                    // --- TEACHER FLOW ---
                    composable("teacher_main") { TeacherDashboard(navController) }
                    composable("teacher_profile") { TeacherProfileScreen(navController) }
                    composable("teacher_edit_profile") { TeacherEditProfileScreen(navController) }
                    composable("teacher_search") { TeacherSearchScreen(navController) }
                    composable("teacher_notification") { TeacherNotificationScreen(navController) }
                    composable("teacher_record") { TeacherReservationRecordScreen(navController) }

                    composable("teacher_facility/{id}") {
                        TeacherFacilityDetailScreen(navController, it.arguments?.getString("id")?:"")
                    }

                    composable(
                        route = "teacher_reserve/{id}?oldId={oldId}",
                        arguments = listOf(
                            navArgument("id") { type = NavType.StringType },
                            navArgument("oldId") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) { backStackEntry ->
                        val facilityId = backStackEntry.arguments?.getString("id") ?: ""
                        val oldResId = backStackEntry.arguments?.getString("oldId")
                        TeacherReservationScreen(navController, facilityId, oldResId)
                    }

                    composable(
                        route = "reservation_detail/{id}",
                        arguments = listOf(navArgument("id") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val reservationId = backStackEntry.arguments?.getString("id") ?: ""
                        TeacherReservationDetailScreen(navController, reservationId)
                    }
                }
            }
        }
    }
}