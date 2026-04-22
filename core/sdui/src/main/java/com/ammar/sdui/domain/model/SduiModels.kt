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
@SerialName("screen")
data class SduiScreen(
    @SerialName("screen_id")
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
    @SerialName("text_content")
    val textContent: String
) : SduiComponent()

@Serializable
@SerialName("icon_button")
data class SduiIconButton(
    @SerialName("icon_name") val iconName: String,
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
    @SerialName("image_url")
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
    @SerialName("action_button")
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
    @SerialName("carousel_id")
    val carouselId: String,
    @SerialName("item_type")
    val itemType: String,
    val items: List<SduiComponent>
) : SduiComponent()

@Serializable
@SerialName("movie_card")
data class SduiMovieCard(
    @SerialName("movie_id")
    val movieId: String,
    @SerialName("poster_url")
    val posterUrl: String,
    val title: SduiComponent,
    val action: SduiAction? = null
) : SduiComponent()

@Serializable
@SerialName("vertical_list")
data class SduiVerticalList(
    @SerialName("list_id")
    val listId: String? = null,
    val items: List<SduiListItem>
) : SduiComponent()

@Serializable
@SerialName("movie_list_item")
data class SduiListItem(
    @SerialName("movie_id")
    val movieId: String,
    @SerialName("poster_url")
    val posterUrl: String,
    val title: SduiText,
    val subtitle: SduiText,
    val action: SduiAction? = null
) : SduiComponent()