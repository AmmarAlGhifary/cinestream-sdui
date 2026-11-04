package com.ammar.sdui.domain.model

import io.github.ammaralghifary.blueprint.model.SduiAction
import io.github.ammaralghifary.blueprint.model.SduiComponent
import io.github.ammaralghifary.blueprint.model.SduiModifier
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/*
 * CineStream's own SDUI vocabulary, built on Blueprint's open SduiComponent / SduiAction.
 *
 * The JSON `type` strings are not declared here: they are bound in CineStreamRegistry, which is the
 * single place that maps a wire name to a model and a composable. The field names (@SerialName) are
 * unchanged from the previous hand-written engine, so the sdui-backend payloads stay valid.
 *
 * Navigation uses Blueprint's NavigateAction (registered as "navigation_action").
 */

// region Actions

@Serializable
data class ApiAction(
    val endpoint: String,
    val method: String,
    val body: Map<String, String>? = null
) : SduiAction()

@Serializable
data class ShareAction(
    val content: String
) : SduiAction()

// endregion

// region Layout

/** Screen chrome: the optional toolbar and the body. Built by [com.ammar.sdui.data.repository.SduiRepositoryImpl]. */
@Serializable
data class SduiScaffold(
    val toolbar: SduiComponent? = null,
    val body: SduiComponent,
    override val modifier: SduiModifier? = null
) : SduiComponent()

@Serializable
data class SduiAppBar(
    val title: SduiComponent,
    val actions: List<SduiComponent> = emptyList(),
    override val modifier: SduiModifier? = null
) : SduiComponent()

@Serializable
data class SduiColumn(
    val children: List<SduiComponent> = emptyList(),
    override val modifier: SduiModifier? = null
) : SduiComponent()

@Serializable
data class SduiSectionHeader(
    val title: SduiComponent,
    @SerialName("action_button")
    val actionButton: SduiComponent? = null,
    override val modifier: SduiModifier? = null
) : SduiComponent()

@Serializable
data class SduiSectionHeaderDetail(
    val title: SduiComponent,
    override val modifier: SduiModifier? = null
) : SduiComponent()

@Serializable
data class SduiVerticalList(
    @SerialName("list_id")
    val listId: String? = null,
    val items: List<SduiComponent> = emptyList(),
    override val modifier: SduiModifier? = null
) : SduiComponent()

// endregion

// region Content

@Serializable
data class SduiText(
    @SerialName("text_content")
    val textContent: String,
    override val modifier: SduiModifier? = null
) : SduiComponent()

@Serializable
data class SduiToolbarTitle(
    @SerialName("text_content")
    val textContent: String,
    val alignment: String? = "start",
    val style: String? = "headline",
    override val modifier: SduiModifier? = null
) : SduiComponent()

// endregion

// region Media

@Serializable
data class SduiFeaturedHero(
    @SerialName("image_url")
    val imageUrl: String,
    val title: SduiComponent,
    val description: SduiComponent,
    val buttons: List<SduiComponent> = emptyList(),
    override val modifier: SduiModifier? = null
) : SduiComponent()

@Serializable
data class SduiFeaturedHeroDetail(
    @SerialName("image_url")
    val imageUrl: String,
    val title: SduiComponent,
    val description1: SduiComponent,
    val description2: SduiComponent,
    val description3: SduiComponent,
    override val modifier: SduiModifier? = null
) : SduiComponent()

@Serializable
data class SduiMovieCarousel(
    @SerialName("carousel_id")
    val carouselId: String,
    @SerialName("item_type")
    val itemType: String,
    val items: List<SduiComponent> = emptyList(),
    override val modifier: SduiModifier? = null
) : SduiComponent()

@Serializable
data class SduiMovieCard(
    @SerialName("movie_id")
    val movieId: String,
    @SerialName("poster_url")
    val posterUrl: String,
    val title: SduiComponent,
    val action: SduiAction? = null,
    override val modifier: SduiModifier? = null
) : SduiComponent()

@Serializable
data class SduiListItem(
    @SerialName("movie_id")
    val movieId: String,
    @SerialName("poster_url")
    val posterUrl: String,
    val title: SduiText,
    val subtitle: SduiText,
    val action: SduiAction? = null,
    override val modifier: SduiModifier? = null
) : SduiComponent()

// endregion

// region Interactive

@Serializable
data class SduiButton(
    val text: String,
    val style: String = "primary",
    val action: SduiAction? = null,
    override val modifier: SduiModifier? = null
) : SduiComponent()

@Serializable
data class SduiTextButton(
    val text: String,
    val action: SduiAction? = null,
    override val modifier: SduiModifier? = null
) : SduiComponent()

@Serializable
data class SduiIconButton(
    @SerialName("icon_name") val iconName: String,
    val action: SduiAction? = null,
    override val modifier: SduiModifier? = null
) : SduiComponent()

// endregion
