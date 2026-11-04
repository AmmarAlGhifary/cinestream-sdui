package com.ammar.detail.state

import io.github.ammaralghifary.blueprint.model.SduiScreen

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(val screen: SduiScreen) : DetailUiState
    data class Error(val message: String) : DetailUiState
}