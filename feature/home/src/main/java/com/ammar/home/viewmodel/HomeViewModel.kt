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

    private val _UiState = MutableStateFlow<HomeUiState>(HomeUiState.Loading)
    val uiState: StateFlow<HomeUiState> = _UiState.asStateFlow()

    init {
        fetcHomeScreen()
    }

    private fun fetcHomeScreen() {
        viewModelScope.launch {
            _UiState.value = HomeUiState.Loading
            getSduiScreenUseCase("home_screen").collect { result ->
                result.fold(
                    onSuccess = { sduiScreen ->
                        _UiState.value = HomeUiState.Success(sduiScreen)
                    },
                    onFailure = { error ->
                        _UiState.value = HomeUiState.Error(error.message ?: "Unknown Error Ocurred")
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
            // TODO Add other actions here later
        }
    }
}