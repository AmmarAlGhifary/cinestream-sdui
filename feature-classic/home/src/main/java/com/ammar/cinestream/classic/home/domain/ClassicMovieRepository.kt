package com.ammar.cinestream.classic.home.domain

import com.ammar.cinestream.core.network.source.tmdb.TmdbApiService
import com.ammar.network.source.tmdb.dto.TmdbMovieDto
import javax.inject.Inject

class ClassicMovieRepository @Inject constructor(
    private val tmdbApiService: TmdbApiService
) {
    suspend fun getTrendingMovies(): List<TmdbMovieDto> {
        return tmdbApiService.getTrendingMovies().results
    }

    suspend fun getUpcomingMovies(): List<TmdbMovieDto> {
        return tmdbApiService.getUpcomingMovies().results
    }
}
