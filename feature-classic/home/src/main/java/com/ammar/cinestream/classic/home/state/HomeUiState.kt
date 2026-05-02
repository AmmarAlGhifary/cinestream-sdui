package com.ammar.cinestream.classic.home.state

import android.graphics.Movie

sealed interface HomeUiState {
    object Loading : HomeUiState
    data class Success(val movies: List<Movie>) : HomeUiState
    data class Error(val message: String) : HomeUiState


}