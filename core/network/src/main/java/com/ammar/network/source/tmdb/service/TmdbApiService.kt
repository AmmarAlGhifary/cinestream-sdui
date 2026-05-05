package com.ammar.cinestream.core.network.source.tmdb

import android.os.Binder
import com.ammar.cinestream.core.network.BuildConfig
import com.ammar.network.source.tmdb.dto.TmdbMovieDto
import com.ammar.network.source.tmdb.dto.TmdbResponseDto
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface TmdbApiService {

    @GET("trending/movie/day")
    suspend fun getTrendingMovies(
        @Query("api_key")
        apiKey: String = BuildConfig.API_KEY_TMDB
    ): TmdbResponseDto

    @GET("movie/upcoming")
    suspend fun getUpcomingMovies(
        @Query("api_key")
        apiKey: String = BuildConfig.API_KEY_TMDB
    ): TmdbResponseDto

    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("api_key")
        apiKey: String = BuildConfig.API_KEY_TMDB
    ): TmdbMovieDto

    @GET("movie/{movie_id}/similar")
    suspend fun getSimilarMovies(
        @Path("movie_id") movieId: Int,
        @Query("api_key")
        apiKey: String = BuildConfig.API_KEY_TMDB
    ): TmdbResponseDto
}
