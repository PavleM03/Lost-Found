package com.pavle.lostandfound

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pavle.lostandfound.ui.screens.*
import com.pavle.lostandfound.ui.theme.LostAndFoundTheme
import com.pavle.lostandfound.ui.viewmodel.AuthViewModel
import com.pavle.lostandfound.ui.viewmodel.MapViewModel
import com.pavle.lostandfound.utils.NotificationHelper

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        NotificationHelper(this).createNotificationChannel()

        setContent {
            LostAndFoundTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val authViewModel: AuthViewModel = viewModel()
    val mapViewModel: MapViewModel = viewModel()
    val context = LocalContext.current

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLogin = { email, password ->
                    authViewModel.login(email, password) { success, message ->
                        if (success) {
                            Toast.makeText(context, "Prijava uspešna!", Toast.LENGTH_SHORT).show()
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            Toast.makeText(context, "Greška: $message", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onNavigateToRegister = {
                    navController.navigate("register")
                }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegister = { email, password, ime, prezime, brojTelefona, imageUri ->
                    authViewModel.register(email, password, ime, prezime, brojTelefona, imageUri) { success, message ->
                        if (success) {
                            Toast.makeText(context, "Registracija uspešna!", Toast.LENGTH_SHORT).show()
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        } else {
                            Toast.makeText(context, "Greška: $message", Toast.LENGTH_LONG).show()
                        }
                    }
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
        composable("home") {
            MapScreen(
                mapViewModel = mapViewModel,
                onNavigateToItemList = { navController.navigate("itemList") },
                onNavigateToLeaderboard = { navController.navigate("leaderboard") }
            )
        }
        composable("itemList") {
            ItemListScreen(
                mapViewModel = mapViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("leaderboard") {
            LeaderboardScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}