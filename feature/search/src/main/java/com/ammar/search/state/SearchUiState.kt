package com.ammar.search.state

import io.github.ammaralghifary.blueprint.model.SduiScreen

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val screen: SduiScreen) : SearchUiState()
    data class Error(val message: String) : SearchUiState()
}
