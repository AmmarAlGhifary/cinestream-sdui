package com.ammar.sdui.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
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
import com.ammar.sdui.domain.model.SduiButton
import com.ammar.sdui.domain.model.SduiIconButton
import com.ammar.sdui.domain.model.SduiSectionHeader
import com.ammar.sdui.domain.model.SduiSectionHeaderDetail
import com.ammar.sdui.domain.model.SduiText
import com.ammar.sdui.domain.model.SduiTextButton
import io.github.ammaralghifary.blueprint.render.SduiScope


@Composable
fun SduiButtonComponent(
    model: SduiButton,
    modifier: Modifier,
    scope: SduiScope
) {
    Button(
        onClick = { model.action?.let(scope::dispatch) },
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
    modifier: Modifier,
    scope: SduiScope
) {
    TextButton(
        onClick = { model.action?.let(scope::dispatch) },
        modifier = modifier
    ) {
        Text(text = model.text, color = MaterialTheme.colorScheme.secondary)
    }
}

@Composable
fun SduiIconButtonComponent(
    model: SduiIconButton,
    modifier: Modifier,
    scope: SduiScope
) {
    val imageVector = when (model.iconName) {
        "search" -> Icons.Default.Search
        "favorite_border" -> Icons.Default.FavoriteBorder
        "share" -> Icons.Default.Share
        else -> Icons.Default.Warning //Fallback
    }
    IconButton(
        onClick = {
            android.util.Log.d(
                "SduiIconButton",
                "Clicked: ${model.iconName}, action: ${model.action}"
            )
            model.action?.let(scope::dispatch)
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = model.iconName,
            tint = MaterialTheme.colorScheme.onBackground
        )
    }
}

@Composable
fun SduiSectionHeaderComponent(
    model: SduiSectionHeader,
    modifier: Modifier,
    scope: SduiScope
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (model.title is SduiText) {
            Text(
                text = model.title.textContent,
                modifier = Modifier.padding(top = 5.dp, start = 3.dp),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        } else {
            scope.Render(model.title)
        }

        model.actionButton?.let { button ->
            scope.Render(button)
        }
    }
}

@Composable
fun SduiSectionHeaderDetailComponent(
    model: SduiSectionHeaderDetail,
    modifier: Modifier,
    scope: SduiScope
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (model.title is SduiText) {
            Text(
                text = model.title.textContent,
                modifier = Modifier.padding(top = 5.dp, start = 10.dp),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        } else {
            scope.Render(model.title)
        }
    }
}