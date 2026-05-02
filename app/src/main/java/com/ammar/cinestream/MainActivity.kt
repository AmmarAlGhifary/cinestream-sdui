package com.ammar.cinestream

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ammar.designsystems.theme.CinestreamTheme
import com.ammar.detail.ui.DetailScreen
import com.ammar.home.ui.HomeScreen
import com.ammar.listmovie.ui.ListMovieScreen
import com.ammar.search.ui.SearchScreen
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
                    startDestination = "home",
                ) {

                    composable("home") {
                        HomeScreen(
                            onNavigateToDetail = { movieId ->
                                navController.navigate("detail/$movieId")
                            },
                            onNavigateToList = { listType ->
                                navController.navigate("movie_list_screen/$listType")
                            },
                            onNavigateToSearch = {
                                navController.navigate("search_screen")
                            }
                        )
                    }

                    composable("search_screen") {
                        SearchScreen(
                            onNavigateToDetail = { movieId ->
                                navController.navigate("detail/$movieId")
                            }
                        )
                    }

                    composable(
                        route = "detail/{movie_id}",
                        arguments = listOf(
                            navArgument("movie_id") { type = NavType.StringType }
                        )
                    ) {
                        DetailScreen(
                            onNavigateToDetail = { movieId ->
                                navController.navigate("detail/$movieId")
                            },
                            onNavigateToList = { listType, movieId ->
                                navController.navigate("movie_list_screen/$listType?movie_id=$movieId")
                            })
                    }

                    composable(
                        route = "movie_list_screen/{list_type}?movie_id={movie_id}",
                        arguments = listOf(
                            navArgument("list_type") { type = NavType.StringType },
                            navArgument("movie_id") {
                                type = NavType.StringType
                                nullable = true
                                defaultValue = null
                            }
                        )
                    ) {
                        ListMovieScreen(
                            onNavigateToDetail = { movieId ->
                                navController.navigate("detail/$movieId")
                            })
                    }
                }
            }
        }
    }
}