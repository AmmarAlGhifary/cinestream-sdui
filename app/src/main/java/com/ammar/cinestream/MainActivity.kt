package com.ammar.cinestream

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ammar.cinestream.auth.SignInHost
import com.ammar.cinestream.auth.SignInViewModel
import com.ammar.cinestream.auth.openInCustomTab
import com.ammar.designsystems.CinestreamTheme
import com.ammar.detail.ui.DetailScreen
import com.ammar.home.ui.HomeScreen
import com.ammar.listmovie.ui.ListMovieScreen
import com.ammar.search.ui.SearchScreen
import com.ammar.sdui.presentation.LocalSduiBlueprint
import dagger.hilt.android.AndroidEntryPoint
import io.github.ammaralghifary.blueprint.Blueprint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** The same instance the SDUI repository decodes with. */
    @Inject
    lateinit var blueprint: Blueprint

    private val signInViewModel: SignInViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Only a new launch: a recreated activity, or one reopened from Recents, still holds the old
        // link, which has already been used.
        val fromHistory = intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        if (savedInstanceState == null && !fromHistory) handleLink(intent)
        setContent {
            CompositionLocalProvider(LocalSduiBlueprint provides blueprint) {
                CinestreamTheme {
                    val navController = rememberNavController()

                    SignInHost(signInViewModel, openPage = ::openInCustomTab)

                    NavHost(
                        navController = navController,
                        startDestination = "home",
                    ) {

                        composable("home") {
                            HomeScreen(
                                onSignIn = signInViewModel::signIn,
                                onNavigateToDetail = { movieId ->
                                    navController.navigate("detail/$movieId")
                                },
                                onNavigateToList = { listType ->
                                    navController.navigate(movieListRoute(listType))
                                },
                                onNavigateToSearch = {
                                    navController.navigate("search_screen")
                                }
                            )
                        }

                        composable("search_screen") {
                            SearchScreen(
                                onSignIn = signInViewModel::signIn,
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
                                onSignIn = signInViewModel::signIn,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToDetail = { movieId ->
                                    navController.navigate("detail/$movieId")
                                },
                                onNavigateToList = { listType, movieId ->
                                    navController.navigate(movieListRoute(listType, movieId))
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
                                onSignIn = signInViewModel::signIn,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToDetail = { movieId ->
                                    navController.navigate("detail/$movieId")
                                })
                        }
                    }
                }
            }
        }
    }

    /** The activity is singleTask, so the sign-in redirect arrives here and closes the browser tab. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLink(intent)
    }

    private fun handleLink(intent: Intent) {
        if (intent.action == Intent.ACTION_VIEW) intent.dataString?.let(signInViewModel::onLink)
    }
}
