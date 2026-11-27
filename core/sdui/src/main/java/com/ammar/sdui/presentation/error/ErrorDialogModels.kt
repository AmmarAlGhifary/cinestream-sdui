package com.ammar.sdui.presentation.error

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ammar.cinestream.core.sdui.BuildConfig
import com.ammar.designsystems.ErrorDialogModel
import com.ammar.designsystems.ErrorSecondaryAction
import com.ammar.network.error.AppError

/**
 * [error] as the model that ErrorDialog shows: [ErrorMapper] decides, this only looks the texts up.
 * Nothing is drawn here.
 */
@Composable
fun errorDialogModel(
    error: AppError,
    secondaryAction: ErrorSecondaryAction = ErrorSecondaryAction.CLOSE,
    showTechnicalDetail: Boolean = BuildConfig.DEBUG
): ErrorDialogModel {
    val content = ErrorMapper.map(error, includeTechnicalDetail = showTechnicalDetail)
    return ErrorDialogModel(
        title = content.serverTitle ?: stringResource(content.titleRes),
        message = content.serverMessage ?: stringResource(content.messageRes),
        retryable = content.retryable,
        secondaryAction = secondaryAction,
        retryAfterSeconds = content.retryAfterSeconds,
        technicalDetail = content.technicalDetail
    )
}
