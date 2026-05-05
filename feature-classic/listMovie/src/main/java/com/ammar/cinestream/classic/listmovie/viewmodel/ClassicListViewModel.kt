package com.ammar.cinestream.classic.listmovie.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.cinestream.classic.listmovie.state.ListMovieUiState
import com.ammar.cinestream.core.network.source.tmdb.TmdbApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClassicListViewModel @Inject constructor(
    private val tmdbApiService: TmdbApiService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<ListMovieUiState>(ListMovieUiState.Loading)
    val uiState: StateFlow<ListMovieUiState> = _uiState.asStateFlow()

    init {
        val listType = savedStateHandle.get<String>("list_type")
        val movieId = savedStateHandle.get<String>("movie_id")

        if (listType != null) {
            fetchList(listType, movieId)
        } else {
            _uiState.value = ListMovieUiState.Error("Missing list_type")
        }
    }

    private fun fetchList(listType: String, movieId: String?) {
        viewModelScope.launch {
            _uiState.value = ListMovieUiState.Loading
            try {
                val (title, response) = when (listType) {
                    "trending" -> "Trending Movies" to tmdbApiService.getTrendingMovies()
                    "upcoming" -> "Upcoming Movies" to tmdbApiService.getUpcomingMovies()
                    "similar" -> {
                        if (movieId == null) throw IllegalArgumentException("Missing movie ID for similar movies")
                        "Similar Movies" to tmdbApiService.getSimilarMovies(movieId.toInt())
                    }
                    else -> throw IllegalArgumentException("Unknown list type: $listType")
                }

                _uiState.value = ListMovieUiState.Success(title, response.results)
            } catch (e: Exception) {
                _uiState.value = ListMovieUiState.Error(e.message ?: "Unknown Error")
            }
        }
    }
}
