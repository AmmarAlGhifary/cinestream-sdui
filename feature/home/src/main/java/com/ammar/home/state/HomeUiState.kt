package com.ammar.home.state

import com.ammar.sdui.domain.model.SduiScreen

sealed interface HomeUiState {
    data object Loading : HomeUiState
    data class Success(val screen: SduiScreen) : HomeUiState
    data class Error(val message: String) : HomeUiState
}