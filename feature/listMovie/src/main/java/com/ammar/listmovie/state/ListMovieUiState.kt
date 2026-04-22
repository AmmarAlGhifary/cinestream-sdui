package com.ammar.listmovie.state

import com.ammar.sdui.domain.model.SduiScreen

sealed interface ListMovieUiState {
    data object Loading: ListMovieUiState
    data class Success(val screen: SduiScreen): ListMovieUiState
    data class Error(val message: String) : ListMovieUiState
}