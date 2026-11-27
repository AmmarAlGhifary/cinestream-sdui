package com.ammar.designsystems

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.ammar.cinestream.core.designsystems.R

/** What the second button offers. The caller's onDismiss has to do what this promises. */
enum class ErrorSecondaryAction { CLOSE, GO_BACK }

/**
 * Everything an error dialog says, already in the user's language.
 *
 * @param retryable false leaves out the Retry button.
 * @param retryAfterSeconds how long the server asked the user to wait, if it did.
 * @param technicalDetail an extra line for debug builds. It must be null in release.
 */
@Immutable
data class ErrorDialogModel(
    val title: String,
    val message: String,
    val retryable: Boolean,
    val secondaryAction: ErrorSecondaryAction = ErrorSecondaryAction.CLOSE,
    val retryAfterSeconds: Int? = null,
    val technicalDetail: String? = null
)

/**
 * The only place an error dialog is drawn, so changing how one looks means changing this file.
 *
 * @param onDismiss the secondary button, also run by the system back button. A tap outside does
 * nothing, so nobody leaves a screen by accident.
 */
@Composable
fun ErrorDialog(
    model: ErrorDialogModel,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val secondaryButton: @Composable () -> Unit = {
        val label = when (model.secondaryAction) {
            ErrorSecondaryAction.CLOSE -> R.string.ds_close
            ErrorSecondaryAction.GO_BACK -> R.string.ds_go_back
        }
        TextButton(onClick = onDismiss) { Text(stringResource(label)) }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = modifier,
        properties = DialogProperties(dismissOnClickOutside = false),
        title = { Text(model.title) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(model.message)
                model.retryAfterSeconds?.let { Text(retryAfterText(it)) }
                model.technicalDetail?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall)
                }
            }
        },
        confirmButton = {
            if (model.retryable) {
                TextButton(onClick = onRetry) { Text(stringResource(R.string.ds_retry)) }
            } else {
                secondaryButton()
            }
        },
        dismissButton = if (model.retryable) secondaryButton else null
    )
}

/** Waits of a minute or more are given in whole minutes, rounded up so nobody retries too early. */
@Composable
private fun retryAfterText(seconds: Int): String =
    if (seconds < 60) {
        pluralStringResource(R.plurals.ds_retry_after_seconds, seconds, seconds)
    } else {
        val minutes = (seconds + 59) / 60
        pluralStringResource(R.plurals.ds_retry_after_minutes, minutes, minutes)
    }
