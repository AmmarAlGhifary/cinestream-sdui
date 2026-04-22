package com.ammar.detail.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ammar.detail.state.DetailUiState
import com.ammar.detail.viewmodel.DetailViewModel
import com.ammar.sdui.presentation.registry.UiComponentRenderer

@Composable
fun DetailScreen(modifier: Modifier = Modifier, viewModel: DetailViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when (val state = uiState) {
        is DetailUiState.Loading -> {
            Box(modifier = modifier, contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        is DetailUiState.Success -> {
            UiComponentRenderer(
                modifier = modifier.fillMaxSize(),
                component = state.screen,
                onAction = viewModel::handleAction)
        }

        is DetailUiState.Error -> {
            Box(modifier = modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(text = "Failed to load details: ${state.message}")
            }
        }
    }
}