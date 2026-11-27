package com.ammar.detail.state

import com.ammar.network.error.AppError
import io.github.ammaralghifary.blueprint.model.SduiScreen

sealed interface DetailUiState {
    data object Loading : DetailUiState

    /** @param refreshError set when a refresh failed; [screen] stays on display. */
    data class Success(val screen: SduiScreen, val refreshError: AppError? = null) : DetailUiState

    /** Nothing could be loaded. [showDialog] turns false once the user has chosen to leave. */
    data class Error(val error: AppError, val showDialog: Boolean = true) : DetailUiState
}
