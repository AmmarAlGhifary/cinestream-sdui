package com.ammar.listmovie.ui


import android.text.Selection
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ammar.listmovie.state.ListMovieUiState
import com.ammar.listmovie.viewmodel.ListMovieViewModel
import com.ammar.sdui.domain.model.NavigationAction
import com.ammar.sdui.presentation.registry.UiComponentRenderer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ListMovieScreen(
    modifier: Modifier = Modifier,
    viewModel: ListMovieViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refreshScreen() },
        modifier = modifier.fillMaxSize()
    ) {
        when (val state = uiState) {
            is ListMovieUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            is ListMovieUiState.Error -> {
                Box(modifier =
                    Modifier.align(Alignment.Center)) {
                    SelectionContainer {
                        Text(text = "Failed to load list: ${state.message}", modifier = Modifier.padding(16.dp))
                    }
                }
            }
            is ListMovieUiState.Success -> {
                UiComponentRenderer(
                    component = state.screen,
                    modifier = Modifier.fillMaxSize(),
                    onAction = { action ->
                        if (action is NavigationAction && action.destination == "movie_detail_screen") {
                            val movieId = action.params?.get("movie_id")
                            if (movieId != null) {
                                onNavigateToDetail(movieId)
                            }
                        } else {
                            viewModel.handleAction(action)
                        }
                    }
                )
            }
        }
    }
}