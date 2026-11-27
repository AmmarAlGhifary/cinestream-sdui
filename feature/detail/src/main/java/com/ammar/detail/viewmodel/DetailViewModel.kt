package com.ammar.detail.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.detail.state.DetailUiState
import com.ammar.network.error.AppError
import com.ammar.network.error.toAppError
import com.ammar.sdui.domain.model.ApiAction
import io.github.ammaralghifary.blueprint.model.NavigateAction
import io.github.ammaralghifary.blueprint.model.SduiAction
import com.ammar.sdui.domain.model.ShareAction
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
class DetailViewModel @Inject constructor(
    private val getSduiScreenUseCase: GetSduiScreenUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow<DetailUiState>(DetailUiState.Loading)
    val uiState: StateFlow<DetailUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val movieId: String? = savedStateHandle.get<String>("movie_id")

    private var loadJob: Job? = null

    init {
        load()
    }

    /** The retry action: refreshes content that is already shown, otherwise loads from scratch. */
    fun reload() {
        if (_uiState.value is DetailUiState.Success) refreshScreen() else load()
    }

    fun refreshScreen() {
        val id = movieId ?: return
        hideError()
        _isRefreshing.value = true
        fetch(id)
    }

    fun dismissError() = hideError()

    private fun load() {
        _isRefreshing.value = false
        val id = movieId
        if (id == null) {
            _uiState.value = DetailUiState.Error(AppError.InvalidRequest())
            return
        }
        _uiState.value = DetailUiState.Loading
        fetch(id)
    }

    private fun fetch(id: String) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            getSduiScreenUseCase(
                screenID = "movie_detail_screen",
                params = mapOf("movie_id" to id)
            ).collect { result ->
                result.fold(
                    onSuccess = { _uiState.value = DetailUiState.Success(it) },
                    onFailure = { failure ->
                        val error = failure.toAppError()
                        _uiState.update { state ->
                            if (state is DetailUiState.Success) state.copy(refreshError = error)
                            else DetailUiState.Error(error)
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
                is DetailUiState.Success -> state.copy(refreshError = null)
                is DetailUiState.Error -> state.copy(showDialog = false)
                DetailUiState.Loading -> state
            }
        }
    }

    fun handleAction(
        action: SduiAction,
        onNavigateToDetail: (String) -> Unit,
        onNavigateToList: (String, String?) -> Unit
    ) {
        when (action) {
            is NavigateAction -> {
                when (action.destination) {
                    "movie_detail_screen" -> {
                        val movieId = action.params.get("movie_id")
                        if (movieId != null) onNavigateToDetail(movieId)
                    }
                    "movie_list_screen" -> {
                        val listType = action.params.get("list_type")
                        val movieId = action.params.get("movie_id") // Needed for "similar" lists
                        if (listType != null) onNavigateToList(listType, movieId)
                    }
                }
            }
            is ApiAction, is ShareAction -> {
                android.util.Log.d("SduiAction", "Unhandled action: ${action.javaClass.simpleName}")
            }
        }
    }
}
