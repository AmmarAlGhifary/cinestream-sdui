package com.ammar.sdui.presentation.error

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.ammar.cinestream.core.sdui.BuildConfig
import com.ammar.designsystems.ErrorDialogModel
import com.ammar.designsystems.ErrorPrimaryAction
import com.ammar.designsystems.ErrorSecondaryAction
import com.ammar.network.error.AppError

/**
 * [error] as the model that ErrorDialog shows: [ErrorMapper] decides, this only looks the texts up.
 * Nothing is drawn here.
 *
 * @param secondaryAction what the screen would like the second button to do. An error that asks for
 * sign in always gets Close, so read it back from the model before choosing what onDismiss does.
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
        primaryAction = when {
            content.requiresSignIn -> ErrorPrimaryAction.SIGN_IN
            content.retryable -> ErrorPrimaryAction.RETRY
            else -> null
        },
        secondaryAction = if (content.requiresSignIn) ErrorSecondaryAction.CLOSE else secondaryAction,
        retryAfterSeconds = content.retryAfterSeconds,
        technicalDetail = content.technicalDetail
    )
}
