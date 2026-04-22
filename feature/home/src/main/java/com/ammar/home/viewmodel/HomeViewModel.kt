package com.ammar.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.home.state.HomeUiState
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
class HomeViewModel @Inject constructor(
    private val getSduiScreenUseCase: GetSduiScreenUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        fetcHomeScreen()
    }

    private fun fetcHomeScreen() {
        viewModelScope.launch {
            _uiState.value = HomeUiState.Loading
            getSduiScreenUseCase("home").collect { result ->
                result.fold(
                    onSuccess = { sduiScreen ->
                        _uiState.value = HomeUiState.Success(sduiScreen)
                    },
                    onFailure = { error ->
                        _uiState.value =
                            HomeUiState.Error(error.message ?: "Unknown Error Occurred")
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
        }
    }

    fun refreshScreen() {
        viewModelScope.launch {
            _isRefreshing.value = true
            try {
                getSduiScreenUseCase("home").collect { result ->
                    _uiState.value = HomeUiState.Success(result.getOrThrow())
                    _isRefreshing.value = false
                }
            } catch (e: Exception) {
                _uiState.value = HomeUiState.Error(e.message ?: "Unknown error")
                _isRefreshing.value = false
            }
        }
    }
}