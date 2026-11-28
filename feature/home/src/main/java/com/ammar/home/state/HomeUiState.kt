package com.ammar.home.state

import com.ammar.network.error.AppError
import io.github.ammaralghifary.blueprint.model.SduiScreen

sealed interface HomeUiState {
    data object Loading : HomeUiState

    /** @param refreshError set when a refresh failed; [screen] stays on display. */
    data class Success(val screen: SduiScreen, val refreshError: AppError? = null) : HomeUiState

    /** Nothing could be loaded. [showDialog] turns false once the user has closed the dialog. */
    data class Error(val error: AppError, val showDialog: Boolean = true) : HomeUiState
}
