package com.ammar.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.home.state.HomeUiState
import com.ammar.network.error.toAppError
import com.ammar.sdui.domain.usecase.GetSduiScreenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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

    private var loadJob: Job? = null

    init {
        load()
    }

    /** The retry action: refreshes content that is already shown, otherwise loads from scratch. */
    fun reload() {
        if (_uiState.value is HomeUiState.Success) refreshScreen() else load()
    }

    fun refreshScreen() {
        hideError()
        _isRefreshing.value = true
        fetch()
    }

    fun dismissError() = hideError()

    private fun load() {
        _isRefreshing.value = false
        _uiState.value = HomeUiState.Loading
        fetch()
    }

    private fun fetch() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            getSduiScreenUseCase("home").collect { result ->
                result.fold(
                    onSuccess = { _uiState.value = HomeUiState.Success(it) },
                    onFailure = { failure ->
                        val error = failure.toAppError()
                        _uiState.update { state ->
                            if (state is HomeUiState.Success) state.copy(refreshError = error)
                            else HomeUiState.Error(error)
                        }
                    }
                )
                _isRefreshing.value = false
            }
        }
    }

    private fun hideError() {
        _uiState.update { state ->
            when (state) {
                is HomeUiState.Success -> state.copy(refreshError = null)
                is HomeUiState.Error -> state.copy(showDialog = false)
                HomeUiState.Loading -> state
            }
        }
    }
}
