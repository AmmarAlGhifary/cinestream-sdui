package com.ammar.detail.state

import com.ammar.sdui.domain.model.SduiScreen

sealed interface DetailUiState {
    data object Loading : DetailUiState
    data class Success(val screen: SduiScreen) : DetailUiState
    data class Error(val message: String) : DetailUiState
}