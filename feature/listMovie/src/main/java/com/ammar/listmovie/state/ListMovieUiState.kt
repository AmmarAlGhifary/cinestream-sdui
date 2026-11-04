package com.ammar.listmovie.state

import io.github.ammaralghifary.blueprint.model.SduiScreen

sealed interface ListMovieUiState {
    data object Loading: ListMovieUiState
    data class Success(val screen: SduiScreen): ListMovieUiState
    data class Error(val message: String) : ListMovieUiState
}