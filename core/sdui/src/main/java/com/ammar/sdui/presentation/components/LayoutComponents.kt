package com.ammar.sdui.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ammar.sdui.domain.model.SduiAction
import com.ammar.sdui.domain.model.SduiAppBar
import com.ammar.sdui.domain.model.SduiColumn
import com.ammar.sdui.domain.model.SduiScreen
import com.ammar.sdui.domain.model.SduiText
import com.ammar.sdui.presentation.registry.UiComponentRenderer

@Composable
fun SduiScreenComponent(
    model: SduiScreen,
    modifier: Modifier = Modifier,
    onAction: (SduiAction) -> Unit
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            model.toolbar?.let { toolbarComponent ->
                UiComponentRenderer(component = toolbarComponent, onAction = onAction)
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            UiComponentRenderer(component = model.body, onAction = onAction)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SduiAppBarComponent(
    model: SduiAppBar,
    modifier: Modifier = Modifier,
    onAction: (SduiAction) -> Unit
) {
    TopAppBar(
        title = { UiComponentRenderer(component = model.title, onAction = onAction) },
        actions = {
            model.actions?.forEach { actionComponent ->
                UiComponentRenderer(component = actionComponent, onAction = onAction)
            }
        },
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = colorScheme.background,
            scrolledContainerColor = colorScheme.background,
            titleContentColor = colorScheme.onBackground,
            actionIconContentColor = colorScheme.onBackground
        )
    )
}

@Composable
fun SduiColumnComponent(
    model: SduiColumn,
    modifier: Modifier = Modifier,
    onAction: (SduiAction) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        model.children.forEach { child ->
            UiComponentRenderer(component = child, onAction = onAction)
        }
    }
}

