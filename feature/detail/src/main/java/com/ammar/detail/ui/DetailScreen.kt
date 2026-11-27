package com.ammar.detail.ui

import androidx.compose.foundation.layout.Box
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
import com.ammar.designsystems.ErrorSecondaryAction
import com.ammar.designsystems.RetryPlaceholder
import com.ammar.detail.state.DetailUiState
import com.ammar.detail.viewmodel.DetailViewModel
import com.ammar.sdui.presentation.SduiScreenRenderer
import com.ammar.sdui.presentation.error.errorDialogModel
import com.ammar.sdui.presentation.error.isMovieNotFound

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    modifier: Modifier = Modifier,
    viewModel: DetailViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToList : (String, String?) -> Unit) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    // Hides the dialog before leaving, so a second tap cannot pop another screen.
    val leave = {
        viewModel.dismissError()
        onNavigateBack()
    }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        modifier = modifier.fillMaxSize(),
        onRefresh = { viewModel.refreshScreen() }
    ) {
        when (val state = uiState) {
            is DetailUiState.Loading -> {
                Box(modifier = modifier, contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            is DetailUiState.Success -> {
                SduiScreenRenderer(
                    modifier = modifier.fillMaxSize(),
                    screen = state.screen,
                    onAction = { action ->
                        viewModel.handleAction(
                            action = action,
                            onNavigateToDetail = onNavigateToDetail,
                            onNavigateToList = onNavigateToList
                        )
                    })
                state.refreshError?.let { error ->
                    // The loaded page stays usable, unless the movie itself is gone.
                    val gone = error.isMovieNotFound()
                    val secondary =
                        if (gone) ErrorSecondaryAction.GO_BACK else ErrorSecondaryAction.CLOSE
                    ErrorDialog(
                        model = errorDialogModel(error, secondary),
                        onRetry = viewModel::reload,
                        onDismiss = if (gone) leave else viewModel::dismissError
                    )
                }
            }

            is DetailUiState.Error -> {
                RetryPlaceholder(onRetry = if (state.showDialog) null else viewModel::reload)
                if (state.showDialog) {
                    ErrorDialog(
                        model = errorDialogModel(state.error, ErrorSecondaryAction.GO_BACK),
                        onRetry = viewModel::reload,
                        onDismiss = leave
                    )
                }
            }
        }

    }
}
