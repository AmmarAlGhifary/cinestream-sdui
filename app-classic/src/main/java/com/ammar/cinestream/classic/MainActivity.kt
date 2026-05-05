package com.ammar.cinestream.classic

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ammar.cinestream.classic.detail.ui.ClassicDetailScreen
import com.ammar.cinestream.classic.home.ui.ClassicHomeScreen
import com.ammar.cinestream.classic.listmovie.ui.ClassicListScreen
import com.ammar.designsystems.CinestreamTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CinestreamTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "home"
                ) {
                    composable("home") {
                        ClassicHomeScreen(
                            onNavigateToDetail = { movieId ->
                                navController.navigate("detail/$movieId")
                            },
                            onNavigateToList = { listType ->
                                navController.navigate("list/$listType")
                            }
                        )
                    }

                    composable(
                        route = "detail/{movie_id}",
                        arguments = listOf(
                            navArgument("movie_id") { type = NavType.StringType }
                        )
                    ) {
                        ClassicDetailScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToDetail = { movieId ->
                                navController.navigate("detail/$movieId")
                            },
                            onNavigateToList = { listType, movieId ->
                                navController.navigate("list/$listType?movie_id=$movieId")
                            }
                        )
                    }

                    composable(
                        route = "list/{list_type}?movie_id={movie_id}",
                        arguments = listOf(
                            navArgument("list_type") { type = NavType.StringType },
                            navArgument("movie_id") { 
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) {
                        ClassicListScreen(
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToDetail = { movieId ->
                                navController.navigate("detail/$movieId")
                            }
                        )
                    }
                }
            }
        }
    }
}