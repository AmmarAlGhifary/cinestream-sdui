package com.ammar.cinestream.classic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.ammar.designsystems.CinestreamTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CinestreamTheme {
                val navControl = rememberNavController()

                NavHost(
                    navController = navControl,
                    startDestination = "home"
                ) {
                    composable("home") {
                    }
                }
            }
        }
    }}