package com.ammar.listmovie.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.listmovie.state.ListMovieUiState
import com.ammar.network.error.AppError
import com.ammar.network.error.toAppError
import com.ammar.network.session.SessionRepository
import io.github.ammaralghifary.blueprint.model.NavigateAction
import io.github.ammaralghifary.blueprint.model.SduiAction
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
class ListMovieViewModel @Inject constructor(
    private val getSduiScreenUseCase: GetSduiScreenUseCase,
    private val savedStateHandle: SavedStateHandle,
    sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<ListMovieUiState>(ListMovieUiState.Loading)
    val uiState: StateFlow<ListMovieUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private var loadJob: Job? = null

    init {
        viewModelScope.launch { sessionRepository.signIns.collect { reload() } }
        load()
    }

    /** The retry action: refreshes content that is already shown, otherwise loads from scratch. */
    fun reload() {
        if (_uiState.value is ListMovieUiState.Success) refreshScreen() else load()
    }

    fun refreshScreen() {
        val params = queryParams() ?: return
        hideError()
        _isRefreshing.value = true
        fetch(params)
    }

    fun dismissError() = hideError()

    private fun queryParams(): Map<String, String>? {
        val listType = savedStateHandle.get<String>("list_type") ?: return null
        val movieId = savedStateHandle.get<String>("movie_id") // Needed for "similar" lists
        return buildMap {
            put("list_type", listType)
            if (movieId != null) put("movie_id", movieId)
        }
    }

    private fun load() {
        _isRefreshing.value = false
        val params = queryParams()
        if (params == null) {
            _uiState.value = ListMovieUiState.Error(AppError.InvalidRequest())
            return
        }
        _uiState.value = ListMovieUiState.Loading
        fetch(params)
    }

    private fun fetch(params: Map<String, String>) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            getSduiScreenUseCase(screenID = "movie_list_screen", params = params).collect { result ->
                result.fold(
                    onSuccess = { _uiState.value = ListMovieUiState.Success(it) },
                    onFailure = { failure ->
                        val error = failure.toAppError()
                        _uiState.update { state ->
                            if (state is ListMovieUiState.Success) state.copy(refreshError = error)
                            else ListMovieUiState.Error(error)
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
                is ListMovieUiState.Success -> state.copy(refreshError = null)
                is ListMovieUiState.Error -> state.copy(showDialog = false)
                ListMovieUiState.Loading -> state
            }
        }
    }

    fun handleAction(action: SduiAction) {
        when (action) {
            is NavigateAction -> {
                android.util.Log.d("SduiAction", "Unhandled action: ${action.javaClass.simpleName}")
            }
            else -> {}
        }
    }
}
