package com.ammar.home.classic.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.cinestream.classic.home.domain.ClassicMovieRepository
import com.ammar.cinestream.classic.home.state.HomeUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ClassicHomeViewModel @Inject constructor(
    private val repository: ClassicMovieRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        fetchHomeData()
    }

    private fun fetchHomeData() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            try {
                val trendingDeferred = async { repository.getTrendingMovies() }
                val upcomingDeferred = async { repository.getUpcomingMovies() }

                val trending = trendingDeferred.await()
                val upcoming = upcomingDeferred.await()

                if (trending.isNotEmpty()) {
                    _uiState.value = HomeUiState.Success(
                        heroMovie = trending.first(),
                        trendingMovies = trending,
                        upcomingMovies = upcoming
                    )
                } else {
                    _uiState.value = HomeUiState.Error("No movies found")
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown Error")
            }
        }
    }
}
