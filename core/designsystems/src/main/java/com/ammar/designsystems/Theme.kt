package com.ammar.designsystems

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val CinestreamColorScheme = darkColorScheme(
    primary = CinestreamPrimary,
    background = CinestreamDarkBackground,
    surface = CinestreamSurface,
    onBackground = CinestreamTextWhite,
    onSurface = CinestreamTextWhite,
    secondary = CinestreamTextSecondary
)

@Composable
fun CinestreamTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = CinestreamColorScheme,
        content = content
    )
}