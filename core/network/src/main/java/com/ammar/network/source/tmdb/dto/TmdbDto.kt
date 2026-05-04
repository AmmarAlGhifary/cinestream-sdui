package com.ammar.network.source.tmdb.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class TmdbResponseDto(
    @SerialName("page") val page: Int? = null,
    @SerialName("results") val results: List<TmdbMovieDto> = emptyList()
)

@Serializable
data class TmdbMovieDto(
    @SerialName("id") val id: Int,
    @SerialName("title") val title: String? = null,
    @SerialName("name") val name: String? = null,
    @SerialName("poster_path") val posterPath: String? = null,
    @SerialName("backdrop_path") val backdropPath: String? = null,
    @SerialName("vote_average") val voteAverage: Double? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("overview") val overview: String? = null,
    @SerialName("runtime") val runtime: Int? = null
)
