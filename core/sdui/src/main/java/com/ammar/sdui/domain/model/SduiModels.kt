package com.ammar.sdui.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class SduiComponent

@Serializable
sealed class SduiAction

@Serializable
@SerialName("navigation_action")
data class NavigationAction(
    val destination: String,
    val params: Map<String, String>? = null
) : SduiAction()

@Serializable
@SerialName("playback_action")
data class PlaybackAction(
    val movieId: String
) : SduiAction()

// UI COMPONENT IMPLEMENTATIONS
@Serializable
@SerialName("screen")
data class SduiScreen(
    val screenId: String,
    val toolbar: SduiComponent? = null,
    val body: SduiComponent
) : SduiComponent()

@Serializable
@SerialName("app_bar")
data class SduiAppBar(
    val title: SduiComponent,
    val actions: List<SduiComponent>? = emptyList()
) : SduiComponent()

@Serializable
@SerialName("text")
data class SduiText(
    val textContent: String
) : SduiComponent()

@Serializable
@SerialName("icon_button")
data class SduiIconButton(
    val iconName: String,
    val action: SduiAction? = null
) : SduiComponent()

@Serializable
@SerialName("column")
data class SduiColumn(
    val children: List<SduiComponent>
) : SduiComponent()

@Serializable
@SerialName("featured_hero")
data class SduiFeaturedHero(
    val imageUrl: String,
    val title: SduiComponent,
    val description: SduiComponent,
    val buttons: List<SduiComponent>? = emptyList()
) : SduiComponent()

@Serializable
@SerialName("button")
data class SduiButton(
    val text: String,
    val style: String,
    val action: SduiAction? = null
) : SduiComponent()

@Serializable
@SerialName("section_header")
data class SduiSectionHeader(
    val title: SduiComponent,
    val actionButton: SduiComponent? = null
) : SduiComponent()

@Serializable
@SerialName("text_button")
data class SduiTextButton(
    val text: String,
    val action: SduiAction? = null
) : SduiComponent()

@Serializable
@SerialName("movie_carousel")
data class SduiMovieCarousel(
    val carouselId: String,
    val itemType: String,
    val items: List<SduiComponent>
) : SduiComponent()

@Serializable
@SerialName("movie_card")
data class SduiMovieCard(
    val movieId: String,
    val posterUrl: String,
    val title: SduiComponent,
    val progress: Float? = null,
    val action: SduiAction? = null
) : SduiComponent()