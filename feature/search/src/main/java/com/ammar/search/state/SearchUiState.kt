package com.ammar.search.state

import com.ammar.network.error.AppError
import io.github.ammaralghifary.blueprint.model.SduiScreen

sealed class SearchUiState {
    object Idle : SearchUiState()
    object Loading : SearchUiState()
    data class Success(val screen: SduiScreen) : SearchUiState()
    /** The search failed. [showDialog] turns false once the user has closed the dialog. */
    data class Error(val error: AppError, val showDialog: Boolean = true) : SearchUiState()
}
