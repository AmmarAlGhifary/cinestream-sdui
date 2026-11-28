package com.ammar.home.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ammar.designsystems.ErrorDialog
import com.ammar.designsystems.RetryPlaceholder
import com.ammar.home.state.HomeUiState
import com.ammar.home.viewmodel.HomeViewModel
import com.ammar.network.session.SessionState
import io.github.ammaralghifary.blueprint.model.NavigateAction
import com.ammar.sdui.presentation.SduiScreenRenderer
import com.ammar.sdui.presentation.error.errorDialogModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateToDetail: (String) -> Unit,
    onNavigateToList: (String) -> Unit,
    onNavigateToSearch: () -> Unit,
    onSignIn: () -> Unit
) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val session by viewModel.session.collectAsStateWithLifecycle()

    PullToRefreshBox (
        isRefreshing = isRefreshing,
        modifier = modifier.fillMaxSize(),
        onRefresh = { viewModel.refreshScreen() },
    ) {
        when (val state = uiState) {
            is HomeUiState.Loading -> CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center)
            )
            is HomeUiState.Error -> {
                RetryPlaceholder(onRetry = if (state.showDialog) null else viewModel::reload)
                if (state.showDialog) {
                    ErrorDialog(
                        model = errorDialogModel(state.error),
                        onRetry = viewModel::reload,
                        onDismiss = viewModel::dismissError,
                        onSignIn = {
                            viewModel.dismissError()
                            onSignIn()
                        }
                    )
                }
            }
            is HomeUiState.Success -> {
                SduiScreenRenderer(
                    screen = state.screen,
                    modifier = Modifier.fillMaxSize(),
                    toolbarTrailing = {
                        (session as? SessionState.SignedIn)?.let {
                            AccountMenu(username = it.account.username, onSignOut = viewModel::signOut)
                        }
                    },
                    onAction = { action ->
                        if (action is NavigateAction) {
                            when (action.destination){
                                "movie_detail_screen" -> {
                                    val movieId = action.params.get("movie_id")
                                    if (movieId != null) onNavigateToDetail(movieId)
                                }
                                "movie_list_screen" -> {
                                    val listType = action.params.get("list_type")
                                    if (listType != null) onNavigateToList(listType)
                                }
                                "search_screen", "search" -> {
                                    onNavigateToSearch()
                                }
                            }
                        }
                    }
                )
                state.refreshError?.let { error ->
                    ErrorDialog(
                        model = errorDialogModel(error),
                        onRetry = viewModel::reload,
                        onDismiss = viewModel::dismissError,
                        onSignIn = {
                            viewModel.dismissError()
                            onSignIn()
                        }
                    )
                }
            }
        }
    }
}
