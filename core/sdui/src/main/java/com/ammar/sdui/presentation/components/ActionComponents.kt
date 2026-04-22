package com.ammar.sdui.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ammar.sdui.domain.model.SduiAction
import com.ammar.sdui.domain.model.SduiButton
import com.ammar.sdui.domain.model.SduiIconButton
import com.ammar.sdui.domain.model.SduiSectionHeader
import com.ammar.sdui.domain.model.SduiTextButton
import com.ammar.sdui.presentation.registry.UiComponentRenderer


@Composable
fun SduiButtonComponent(
    model: SduiButton,
    modifier: Modifier = Modifier,
    onAction: (SduiAction) -> Unit
) {
    Button(
        onClick = { model.action?.let { onAction(it) } },
        modifier = modifier
    ) {
        Text(text = model.text)
    }
}

@Composable
fun SduiTextButtonComponent(
    model: SduiTextButton,
    modifier: Modifier = Modifier,
    onAction: (SduiAction) -> Unit
) {
    TextButton(
        onClick = { model.action?.let { onAction(it) } },
        modifier = modifier
    ) {
        Text(text = model.text)
    }
}

@Composable
fun SduiIconButtonComponent(
    model: SduiIconButton,
    modifier: Modifier = Modifier,
    onAction: (SduiAction) -> Unit
) {
    IconButton(
        onClick = { 
            android.util.Log.d("SduiIconButton", "Clicked: ${model.iconName}, action: ${model.action}")
            model.action?.let { onAction(it) } 
        },
        modifier = modifier
    ) {
        // A real app would have an icon mapper here (e.g., mapping "search" string to Icons.Default.Search)
        // Hardcoding search for the Golden Sample
        Icon(imageVector = Icons.Default.Search, contentDescription = model.iconName)
    }
}

@Composable
fun SduiSectionHeaderComponent(
    model: SduiSectionHeader,
    modifier: Modifier = Modifier,
    onAction: (SduiAction) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        UiComponentRenderer(component = model.title, onAction = onAction)
        model.actionButton?.let { button ->
            UiComponentRenderer(component = button, onAction = onAction)
        }
    }
}