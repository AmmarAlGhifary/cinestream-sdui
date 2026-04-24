package com.ammar.sdui.presentation.components

import android.widget.Button
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ammar.sdui.domain.model.SduiAction
import com.ammar.sdui.domain.model.SduiButton
import com.ammar.sdui.domain.model.SduiIconButton
import com.ammar.sdui.domain.model.SduiSectionHeader
import com.ammar.sdui.domain.model.SduiText
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
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text(text = model.text, color = MaterialTheme.colorScheme.onBackground)
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
        Text(text = model.text, color = MaterialTheme.colorScheme.secondary)
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
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (model.title is SduiText) {
            Text(text = model.title.textContent,
                modifier = Modifier.padding(top = 5.dp, start = 3.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground)
        } else {
            UiComponentRenderer(component = model.title, onAction = onAction)
        }

        model.actionButton?.let { button ->
            UiComponentRenderer(component = button, onAction = onAction)
        }
    }
}