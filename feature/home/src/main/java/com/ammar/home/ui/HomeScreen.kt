package com.ammar.home.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ammar.home.state.HomeUiState
import com.ammar.home.viewmodel.HomeViewModel
import com.ammar.sdui.domain.model.NavigationAction
import com.ammar.sdui.presentation.registry.UiComponentRenderer


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(modifier: Modifier = Modifier, viewModel: HomeViewModel = hiltViewModel(), onNavigateToDetail: (String) -> Unit) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    PullToRefreshBox (
        isRefreshing = isRefreshing,
        modifier = modifier.fillMaxSize(),
        onRefresh = { viewModel.refreshScreen() },
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> CircularProgressIndicator()
            is HomeUiState.Error -> SelectionContainer {
                Text(
                    text = "Failed to load: ${state.message}",
                    modifier = Modifier.padding(16.dp)
                )
            }

            is HomeUiState.Success -> UiComponentRenderer(
                component = state.screen,
                modifier = Modifier.fillMaxSize(),
                onAction = { action ->
                    if (action is NavigationAction && action.destination == "movie_detail_screen") {
                        val movieId = action.params?.get("movie_id")
                        if (movieId != null) {
                            onNavigateToDetail(movieId)
                        } else {
                            viewModel.handleAction(action)
                        }
                    }
                }
            )
        }
    }
}

