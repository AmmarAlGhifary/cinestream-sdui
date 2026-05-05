package com.ammar.cinestream.classic.detail.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.cinestream.classic.detail.state.DetailUiState
import com.ammar.cinestream.core.network.source.tmdb.TmdbApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClassicDetailViewModel @Inject constructor(
    private val tmdbApiService: TmdbApiService,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val movieId: String? = savedStateHandle.get<String>("movie_id")

    init {
        fetchDetailData()
    }

    private fun fetchDetailData() {
        if (movieId == null) {
            _uiState.value = DetailUiState.Error("Missing movie ID")
            return
        }

        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            try {
                val id = movieId.toInt()
                val detailsDeferred = async { tmdbApiService.getMovieDetails(id) }
                val similarDeferred = async { tmdbApiService.getSimilarMovies(id) }

                val details = detailsDeferred.await()
                val similar = similarDeferred.await().results

                _uiState.value = DetailUiState.Success(
                    movieDetails = details,
                    similarMovies = similar
                )
            } catch (e: Exception) {
                _uiState.value = DetailUiState.Error(e.message ?: "Unknown Error")
            }
        }
    }
}
