package com.ammar.search.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.network.error.toAppError
import com.ammar.network.session.SessionRepository
import com.ammar.sdui.domain.usecase.GetSduiScreenUseCase
import com.ammar.search.state.SearchUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val getSduiScreenUseCase: GetSduiScreenUseCase,
    sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<SearchUiState>(SearchUiState.Idle)
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private var searchJob: Job? = null

    init {
        viewModelScope.launch { sessionRepository.accountChanges.collect { reload() } }
    }

    fun onQueryChanged(query: String) {
        val capped = query.capped()
        if (capped == _searchQuery.value) return
        _searchQuery.value = capped
        searchJob?.cancel()
        if (capped.isBlank()) {
            _uiState.value = SearchUiState.Idle
            return
        }

        searchJob = viewModelScope.launch {
            delay(500)
            performSearch(capped)
        }
    }

    /** The retry action: runs the current query again, without the typing delay. */
    fun reload() {
        val query = _searchQuery.value
        if (query.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch { performSearch(query) }
    }

    fun dismissError() {
        _uiState.update { if (it is SearchUiState.Error) it.copy(showDialog = false) else it }
    }

    private suspend fun performSearch(query: String) {
        _uiState.value = SearchUiState.Loading
        getSduiScreenUseCase("search", mapOf("query" to query)).collect { result ->
            result.fold(
                onSuccess = { sduiScreen -> _uiState.value = SearchUiState.Success(sduiScreen) },
                onFailure = { _uiState.value = SearchUiState.Error(it.toAppError()) }
            )
        }
    }

    /** The backend rejects longer queries. The cut never splits a surrogate pair, e.g. an emoji. */
    private fun String.capped(): String {
        if (length <= MAX_QUERY_LENGTH) return this
        val cut = take(MAX_QUERY_LENGTH)
        return if (cut.last().isHighSurrogate()) cut.dropLast(1) else cut
    }

    private companion object {
        const val MAX_QUERY_LENGTH = 200
    }
}
