package com.ammar.detail.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.detail.state.DetailUiState
import com.ammar.sdui.domain.model.SduiAction
import com.ammar.sdui.domain.usecase.GetSduiScreenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DetailViewModel @Inject constructor(
    private val getSduiScreenUseCase: GetSduiScreenUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        val movieId = savedStateHandle.get<String>("movie_id")
        if (movieId != null) {
            fetchDetailScreen(movieId)
        } else {
            _uiState.value = DetailUiState.Error("Movie ID is missing from navigation")
        }
    }

    private fun fetchDetailScreen(movieId: String) {
        viewModelScope.launch {
            _uiState.value = DetailUiState.Loading
            getSduiScreenUseCase(
                screenID = "movie_detail_screen",
                params = mapOf("movie_id" to movieId)
            ).collect { result ->
                result.fold(
                    onSuccess = { sduiScreen ->
                        _uiState.value = DetailUiState.Success(sduiScreen)
                    },
                    onFailure = { error ->
                        _uiState.value =
                            DetailUiState.Error(error.message ?: "Unknown Error Occurred")
                    }
                )
            }
        }
    }

    fun handleAction(action: SduiAction) {
        println("Action triggered on Detail Screen: $action")
    }


}