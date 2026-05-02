package com.ammar.listmovie.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.listmovie.state.ListMovieUiState
import com.ammar.sdui.domain.model.NavigationAction
import com.ammar.sdui.domain.model.SduiAction
import com.ammar.sdui.domain.usecase.GetSduiScreenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListMovieViewModel @Inject constructor(
    private val getSduiScreenUseCase: GetSduiScreenUseCase,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<ListMovieUiState>(ListMovieUiState.Loading)
    val uiState: StateFlow<ListMovieUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        val listType = savedStateHandle.get<String>("list_type")
        val movieId = savedStateHandle.get<String>("movie_id")

        if (listType != null) {
            fetchListMovie(listType, movieId)
        } else {
            _uiState.value = ListMovieUiState.Error("Missing list_type parameter")
        }
    }

    private fun fetchListMovie(listType : String, movieId: String?) {
        viewModelScope.launch {
            _uiState.value = ListMovieUiState.Loading
            val queryParams = mutableMapOf("list_type" to listType)
            if (movieId != null) {
                queryParams["movie_id"] = movieId
            }

            getSduiScreenUseCase("movie_list_screen", params = queryParams).collect { result ->
                result.fold(
                    onSuccess = { sduiScreen ->
                        _uiState.value = ListMovieUiState.Success(sduiScreen)
                    },
                    onFailure = { error ->
                        _uiState.value = ListMovieUiState.Error(error.message ?: "Unknown Error Occurred")
                    }
                )
            }
        }
    }

    fun handleAction(action: SduiAction) {
        when (action) {
            is NavigationAction -> {
                println("Navigation Triggered: Destination = ${action.destination}, Params = ${action.params}")
            }
            else -> {}
        }
    }

    fun refreshScreen() {
        viewModelScope.launch {
            _isRefreshing.value = true
            val listType = savedStateHandle.get<String>("list_type") ?: "trending"
            val movieId = savedStateHandle.get<String>("movie_id")

            val queryParams = mutableMapOf("list_type" to listType)
            if (movieId != null) {
                queryParams["movie_id"] = movieId
            }

            try {
                getSduiScreenUseCase(
                    screenID = "movie_list_screen",
                    params = queryParams
                ).collect { result ->
                    _uiState.value = ListMovieUiState.Success(result.getOrThrow())
                    _isRefreshing.value = false
                }
            } catch (e: Exception) {
                _uiState.value = ListMovieUiState.Error(e.message ?: "Unknown error")
                _isRefreshing.value = false
            }
        }
    }
}