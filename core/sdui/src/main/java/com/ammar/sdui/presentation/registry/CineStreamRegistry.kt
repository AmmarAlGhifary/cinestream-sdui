package com.ammar.sdui.presentation.registry

import android.util.Log
import com.ammar.sdui.domain.model.ApiAction
import com.ammar.sdui.domain.model.ShareAction
import com.ammar.sdui.domain.model.SduiAppBar
import com.ammar.sdui.domain.model.SduiButton
import com.ammar.sdui.domain.model.SduiColumn
import com.ammar.sdui.domain.model.SduiFeaturedHero
import com.ammar.sdui.domain.model.SduiFeaturedHeroDetail
import com.ammar.sdui.domain.model.SduiIconButton
import com.ammar.sdui.domain.model.SduiListItem
import com.ammar.sdui.domain.model.SduiMovieCard
import com.ammar.sdui.domain.model.SduiMovieCarousel
import com.ammar.sdui.domain.model.SduiScaffold
import com.ammar.sdui.domain.model.SduiSectionHeader
import com.ammar.sdui.domain.model.SduiSectionHeaderDetail
import com.ammar.sdui.domain.model.SduiText
import com.ammar.sdui.domain.model.SduiTextButton
import com.ammar.sdui.domain.model.SduiToolbarTitle
import com.ammar.sdui.domain.model.SduiVerticalList
import com.ammar.sdui.presentation.components.SdUiTextComponent
import com.ammar.sdui.presentation.components.SduiAppBarComponent
import com.ammar.sdui.presentation.components.SduiButtonComponent
import com.ammar.sdui.presentation.components.SduiColumnComponent
import com.ammar.sdui.presentation.components.SduiFeatureHeroDetailComponent
import com.ammar.sdui.presentation.components.SduiFeaturedHeroComponent
import com.ammar.sdui.presentation.components.SduiIconButtonComponent
import com.ammar.sdui.presentation.components.SduiListItemComponent
import com.ammar.sdui.presentation.components.SduiMovieCardComponent
import com.ammar.sdui.presentation.components.SduiMovieCarouselComponent
import com.ammar.sdui.presentation.components.SduiScaffoldComponent
import com.ammar.sdui.presentation.components.SduiSectionHeaderComponent
import com.ammar.sdui.presentation.components.SduiSectionHeaderDetailComponent
import com.ammar.sdui.presentation.components.SduiTextButtonComponent
import com.ammar.sdui.presentation.components.SduiToolbarTitleComponent
import com.ammar.sdui.presentation.components.SduiVerticalListComponent
import io.github.ammaralghifary.blueprint.Blueprint
import io.github.ammaralghifary.blueprint.model.NavigateAction
import io.github.ammaralghifary.blueprint.registry.sduiRegistry

/**
 * The wire vocabulary of the sdui-backend: every JSON `type` the client understands, bound to its
 * model and composable. Anything not listed here degrades to Blueprint's fallback (a labelled
 * placeholder in debug builds, nothing in release) instead of failing the screen.
 */
val CineStreamRegistry = sduiRegistry {
    // Layout
    component(SCAFFOLD_TYPE, SduiScaffold::class, SduiScaffold.serializer()) { c, mod, scope -> SduiScaffoldComponent(c, mod, scope) }
    component("app_bar", SduiAppBar::class, SduiAppBar.serializer()) { c, mod, scope -> SduiAppBarComponent(c, mod, scope) }
    component("column", SduiColumn::class, SduiColumn.serializer()) { c, mod, scope -> SduiColumnComponent(c, mod, scope) }
    component("section_header", SduiSectionHeader::class, SduiSectionHeader.serializer()) { c, mod, scope -> SduiSectionHeaderComponent(c, mod, scope) }
    component("section_header_detail", SduiSectionHeaderDetail::class, SduiSectionHeaderDetail.serializer()) { c, mod, scope -> SduiSectionHeaderDetailComponent(c, mod, scope) }
    component("vertical_list", SduiVerticalList::class, SduiVerticalList.serializer()) { c, mod, scope -> SduiVerticalListComponent(c, mod, scope) }

    // Content
    component("text", SduiText::class, SduiText.serializer()) { c, mod, _ -> SdUiTextComponent(c, mod) }
    component("toolbar_title", SduiToolbarTitle::class, SduiToolbarTitle.serializer()) { c, mod, _ -> SduiToolbarTitleComponent(c, mod) }

    // Media
    component("featured_hero", SduiFeaturedHero::class, SduiFeaturedHero.serializer()) { c, mod, scope -> SduiFeaturedHeroComponent(c, mod, scope) }
    component("featured_hero_detail", SduiFeaturedHeroDetail::class, SduiFeaturedHeroDetail.serializer()) { c, mod, scope -> SduiFeatureHeroDetailComponent(c, mod, scope) }
    component("movie_carousel", SduiMovieCarousel::class, SduiMovieCarousel.serializer()) { c, mod, scope -> SduiMovieCarouselComponent(c, mod, scope) }
    component("movie_card", SduiMovieCard::class, SduiMovieCard.serializer()) { c, mod, scope -> SduiMovieCardComponent(c, mod, scope) }
    component("movie_list_item", SduiListItem::class, SduiListItem.serializer()) { c, mod, scope -> SduiListItemComponent(c, mod, scope) }

    // Interactive
    component("button", SduiButton::class, SduiButton.serializer()) { c, mod, scope -> SduiButtonComponent(c, mod, scope) }
    component("text_button", SduiTextButton::class, SduiTextButton.serializer()) { c, mod, scope -> SduiTextButtonComponent(c, mod, scope) }
    component("icon_button", SduiIconButton::class, SduiIconButton.serializer()) { c, mod, scope -> SduiIconButtonComponent(c, mod, scope) }

    // Actions
    action("navigation_action", NavigateAction::class, NavigateAction.serializer())
    action("api_action", ApiAction::class, ApiAction.serializer())
    action("share_action", ShareAction::class, ShareAction.serializer())
}

/** Synthetic type the repository wraps `toolbar` + `body` in, see SduiRepositoryImpl. Never sent by the server. */
internal const val SCAFFOLD_TYPE = "screen_scaffold"

/**
 * The one Blueprint instance shared by decoding (repository) and rendering (UI). Decode problems
 * are logged by kind and node type only: `DecodeError.message` carries no payload text, `raw` does.
 */
object CineStreamBlueprint {
    val instance: Blueprint by lazy {
        Blueprint(
            registry = CineStreamRegistry,
            onDecodeError = { Log.w(TAG, "${it.kind} [${it.type}]: ${it.message}") },
            onDecoded = { screen, errors ->
                if (errors.isNotEmpty()) Log.w(TAG, "${screen.id}: decoded with ${errors.size} problem(s)")
            },
        )
    }

    private const val TAG = "Blueprint"
}
