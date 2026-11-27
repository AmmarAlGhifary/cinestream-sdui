package com.ammar.designsystems

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.ammar.cinestream.core.designsystems.R

/**
 * Fills a screen that has nothing to show because loading failed. It scrolls even when empty, which
 * is what lets a surrounding pull-to-refresh fire.
 *
 * @param onRetry null hides the button, e.g. while an error dialog is already offering the choice.
 */
@Composable
fun RetryPlaceholder(
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (onRetry != null) {
            Button(onClick = onRetry) { Text(stringResource(R.string.ds_retry)) }
        }
    }
}
