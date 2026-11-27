package com.ammar.detail.state

import com.ammar.network.error.AppError
import com.ammar.sdui.domain.model.ApiAction
import io.github.ammaralghifary.blueprint.model.SduiScreen

sealed interface DetailUiState {
    data object Loading : DetailUiState

    /**
     * @param refreshError set when a refresh failed; [screen] stays on display.
     * @param actionFailure set when an action such as saving a favorite failed.
     */
    data class Success(
        val screen: SduiScreen,
        val refreshError: AppError? = null,
        val actionFailure: ActionFailure? = null
    ) : DetailUiState

    /** Nothing could be loaded. [showDialog] turns false once the user has chosen to leave. */
    data class Error(val error: AppError, val showDialog: Boolean = true) : DetailUiState
}

/** A failed [action], kept so that it can be sent again. */
data class ActionFailure(val error: AppError, val action: ApiAction)
