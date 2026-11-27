package com.ammar.cinestream.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.ammar.designsystems.ErrorDialog
import com.ammar.sdui.presentation.error.errorDialogModel

/**
 * The part of sign-in that is not on any one screen: opening the approval page and reporting why
 * signing in failed.
 *
 * @param openPage returns false when the page could not be opened.
 */
@Composable
fun SignInHost(
    viewModel: SignInViewModel,
    openPage: (String) -> Boolean
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(viewModel, lifecycleOwner) {
        // Android does not let an app in the background open the browser.
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.approvalPages.collect { if (!openPage(it)) viewModel.onPageUnavailable() }
        }
    }

    val error by viewModel.error.collectAsStateWithLifecycle()
    error?.let {
        ErrorDialog(
            model = errorDialogModel(it),
            onRetry = viewModel::retry,
            onDismiss = viewModel::dismissError,
            onSignIn = viewModel::signIn
        )
    }
}
