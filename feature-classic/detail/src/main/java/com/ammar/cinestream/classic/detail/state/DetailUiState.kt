package com.ammar.cinestream.classic.detail.state

import com.ammar.network.source.tmdb.dto.TmdbMovieDto

sealed class DetailUiState {
    object Loading : DetailUiState()
    data class Success(
        val movieDetails: TmdbMovieDto,
        val similarMovies: List<TmdbMovieDto>
    ) : DetailUiState()
    data class Error(val message: String) : DetailUiState()
}
