package com.ammar.sdui.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.ammar.sdui.domain.model.SduiAction
import com.ammar.sdui.domain.model.SduiFeaturedHero
import com.ammar.sdui.domain.model.SduiFeaturedHeroDetail
import com.ammar.sdui.domain.model.SduiMovieCard
import com.ammar.sdui.domain.model.SduiMovieCarousel
import com.ammar.sdui.domain.model.SduiText
import com.ammar.sdui.domain.model.SduiToolbarTitle
import com.ammar.sdui.presentation.registry.UiComponentRenderer

@Composable
fun SduiFeaturedHeroComponent(
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
        Column(modifier = Modifier.padding(10.dp)) {
            UiComponentRenderer(component = model.title, onAction = onAction)
            Spacer(modifier = Modifier.height(5.dp))
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
fun SduiFeatureHeroDetailComponent(
    model: SduiFeaturedHeroDetail,
    modifier: Modifier = Modifier,
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
    }
    Column(modifier = Modifier.padding(10.dp)) {
        UiComponentRenderer(component = model.title)
        Spacer(modifier = Modifier.height(5.dp))
        UiComponentRenderer(component = model.description1, onAction = {})
        UiComponentRenderer(component = model.description2, onAction = {})
        UiComponentRenderer(component = model.description3, onAction = {})
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
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
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
            .width(130.dp)
            .clickable { model.action?.let { onAction(it) } },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.3f))
    ) {
        Column {
            AsyncImage(
                model = model.posterUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            )
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 12.dp)) {
                if (model.title is SduiText) {
                    Text(
                        text = model.title.textContent,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis)
                } else {
                    UiComponentRenderer(component = model.title, onAction = onAction)
                }
            }
        }
    }
}

@Composable
fun SdUiTextComponent(model: SduiText, modifier: Modifier = Modifier) {
    Text(
        text = model.textContent,
        modifier = modifier.padding(start = 10.dp),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun SduiToolbarTitleComponent(
    model: SduiToolbarTitle,
    modifier: Modifier = Modifier
) {
    val textAlign = when (model.alignment) {
        "center" -> TextAlign.Center
        "end" -> TextAlign.End
        else -> TextAlign.Start
    }

    val textStyle = when (model.style) {
        "subtitle" -> MaterialTheme.typography.titleMedium
        else -> MaterialTheme.typography.titleLarge // Default
    }

    Text(
        text = model.textContent,
        modifier = modifier.fillMaxWidth(),
        textAlign = textAlign,
        style = textStyle,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}
