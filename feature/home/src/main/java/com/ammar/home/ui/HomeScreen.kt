package com.ammar.home.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ammar.home.state.HomeUiState
import com.ammar.home.viewmodel.HomeViewModel
import com.ammar.sdui.presentation.registry.UiComponentRenderer


@Composable
fun HomeScreen(modifier: Modifier = Modifier, viewModel: HomeViewModel = hiltViewModel()) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
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
                onAction = viewModel::handleAction
            )
        }
    }
}

