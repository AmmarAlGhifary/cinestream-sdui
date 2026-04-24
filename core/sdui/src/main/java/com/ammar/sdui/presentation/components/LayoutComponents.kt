package com.ammar.sdui.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ammar.sdui.domain.model.SduiAction
import com.ammar.sdui.domain.model.SduiAppBar
import com.ammar.sdui.domain.model.SduiColumn
import com.ammar.sdui.domain.model.SduiListItem
import com.ammar.sdui.domain.model.SduiScreen
import com.ammar.sdui.domain.model.SduiVerticalList
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

@Composable
fun SduiVerticalListComponent(
    model: SduiVerticalList,
    modifier: Modifier = Modifier,
    onAction: (SduiAction) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(model.items) { item ->
            UiComponentRenderer(component = item, onAction = onAction)
        }
    }
}

@Composable
fun SduiListItemComponent(
    model: SduiListItem,
    modifier: Modifier = Modifier,
    onAction: (SduiAction) -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                model.action?.let { onAction(it) }
            },
        colors = CardDefaults.cardColors(
            containerColor = colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = model.posterUrl,
                contentDescription = "Movie Poster",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .width(100.dp)
                    .height(150.dp)
                    .clip(RoundedCornerShape(8.dp))
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = model.title.textContent, // Note: Use camelCase property name
                    style = MaterialTheme.typography.titleMedium,
                    color = colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = model.subtitle.textContent, // Note: Use camelCase property name
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
    }
}