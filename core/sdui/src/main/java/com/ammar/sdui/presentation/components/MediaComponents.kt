package com.ammar.sdui.presentation.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ammar.sdui.domain.model.SduiAction
import com.ammar.sdui.domain.model.SduiFeaturedHero
import com.ammar.sdui.domain.model.SduiMovieCard
import com.ammar.sdui.domain.model.SduiMovieCarousel
import com.ammar.sdui.domain.model.SduiText
import com.ammar.sdui.presentation.registry.UiComponentRenderer

@Composable
fun SduiFeaturedHeroCompoent(
    model: SduiFeaturedHero,
    modifier: Modifier = Modifier,
    onAction: (SduiAction) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AsyncImage(
            model.imageUrl,
            contentDescription = "Featured Hero Image",
            contentScale = ContentScale.Crop,
            modifier = modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(Color.DarkGray)
        )
        Column(modifier = Modifier.padding(16.dp)) {
            UiComponentRenderer(component = model.title, onAction = onAction)
            Spacer(modifier = Modifier.height(8.dp))
            UiComponentRenderer(component = model.description, onAction = onAction)

            Row(
                modifier = Modifier.padding(top = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                model.buttons?.forEach { button ->
                    UiComponentRenderer(component = button, onAction = onAction)
                }
            }
        }
    }
}

@Composable
fun SduiMovieCarouselComponent(
    model: SduiMovieCarousel,
    modifier: Modifier = Modifier,
    onAction: (SduiAction) -> Unit
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(model.items) { items ->
            UiComponentRenderer(component = items, onAction = onAction)
        }
    }
}

@Composable
fun SduiMovieCardComponent(
    model: SduiMovieCard,
    modifier: Modifier = Modifier,
    onAction: (SduiAction) -> Unit
) {
    Card(
        modifier = modifier
            .width(120.dp)
            .clickable { model.action?.let { onAction(it) } }
    ) {
        Column {
            AsyncImage(
                model = model.posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.DarkGray)
            )
            Box(modifier = Modifier.padding(8.dp)) {
                UiComponentRenderer(component = model.title, onAction = onAction)
            }
        }
    }
}

@Composable
fun SdUiTextComponent(model: SduiText, modifier: Modifier = Modifier) {
    Text(
        text = model.textContent,
        modifier = modifier.padding(8.dp)
    )
}
