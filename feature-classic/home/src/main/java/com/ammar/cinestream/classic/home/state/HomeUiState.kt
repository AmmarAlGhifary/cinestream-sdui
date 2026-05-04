package com.ammar.cinestream.classic.home.state

import com.ammar.network.source.tmdb.dto.TmdbMovieDto

sealed class HomeUiState {
    object Loading : HomeUiState()
    data class Success(
        val heroMovie: TmdbMovieDto,
        val trendingMovies: List<TmdbMovieDto>,
        val upcomingMovies: List<TmdbMovieDto>
    ) : HomeUiState()
    data class Error(val message: String) : HomeUiState()
}
