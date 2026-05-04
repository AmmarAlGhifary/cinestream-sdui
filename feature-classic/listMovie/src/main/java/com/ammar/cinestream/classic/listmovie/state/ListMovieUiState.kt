package com.ammar.cinestream.classic.listmovie.state

import com.ammar.network.source.tmdb.dto.TmdbMovieDto

sealed class ListMovieUiState {
    object Loading : ListMovieUiState()
    data class Success(val title: String, val movies: List<TmdbMovieDto>) : ListMovieUiState()
    data class Error(val message: String) : ListMovieUiState()
}
