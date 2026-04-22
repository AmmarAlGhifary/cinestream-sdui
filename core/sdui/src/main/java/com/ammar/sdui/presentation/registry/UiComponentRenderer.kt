package com.ammar.sdui.presentation.registry

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Red
import androidx.compose.ui.unit.dp
import com.ammar.sdui.domain.model.SduiAction
import com.ammar.sdui.domain.model.SduiAppBar
import com.ammar.sdui.domain.model.SduiButton
import com.ammar.sdui.domain.model.SduiColumn
import com.ammar.sdui.domain.model.SduiComponent
import com.ammar.sdui.domain.model.SduiFeaturedHero
import com.ammar.sdui.domain.model.SduiIconButton
import com.ammar.sdui.domain.model.SduiListItem
import com.ammar.sdui.domain.model.SduiMovieCard
import com.ammar.sdui.domain.model.SduiMovieCarousel
import com.ammar.sdui.domain.model.SduiScreen
import com.ammar.sdui.domain.model.SduiSectionHeader
import com.ammar.sdui.domain.model.SduiText
import com.ammar.sdui.domain.model.SduiTextButton
import com.ammar.sdui.domain.model.SduiVerticalList
import com.ammar.sdui.presentation.components.SdUiTextComponent
import com.ammar.sdui.presentation.components.SduiAppBarComponent
import com.ammar.sdui.presentation.components.SduiButtonComponent
import com.ammar.sdui.presentation.components.SduiColumnComponent
import com.ammar.sdui.presentation.components.SduiFeaturedHeroCompoent
import com.ammar.sdui.presentation.components.SduiIconButtonComponent
import com.ammar.sdui.presentation.components.SduiListItemComponent
import com.ammar.sdui.presentation.components.SduiMovieCardComponent
import com.ammar.sdui.presentation.components.SduiMovieCarouselComponent
import com.ammar.sdui.presentation.components.SduiScreenComponent
import com.ammar.sdui.presentation.components.SduiSectionHeaderComponent
import com.ammar.sdui.presentation.components.SduiTextButtonComponent
import com.ammar.sdui.presentation.components.SduiVerticalList

@Composable
fun UiComponentRenderer(
    component: SduiComponent,
    modifier: Modifier = Modifier,
    onAction: (SduiAction) -> Unit = {}
) {
    when (component) {
        // Screen
        is SduiScreen -> SduiScreenComponent(component, modifier, onAction)

        // Layouts
        is SduiColumn -> SduiColumnComponent(component, modifier, onAction)
        is SduiAppBar -> SduiAppBarComponent(component, modifier, onAction)
        is SduiSectionHeader -> SduiSectionHeaderComponent(component, modifier, onAction)
        is SduiVerticalList -> SduiVerticalList(model = component, modifier = modifier, onAction = onAction)
        is SduiListItem -> SduiListItemComponent(model = component, modifier = modifier, onAction = onAction)        //Content and Media
        is SduiFeaturedHero -> SduiFeaturedHeroCompoent(component, modifier, onAction)
        is SduiMovieCarousel -> SduiMovieCarouselComponent(component, modifier, onAction)
        is SduiMovieCard -> SduiMovieCardComponent(component, modifier, onAction)
        is SduiText -> SdUiTextComponent(model = component, modifier = modifier)

        // Interactions
        is SduiButton -> SduiButtonComponent(component, modifier, onAction)
        is SduiTextButton -> SduiTextButtonComponent(component, modifier, onAction)
        is SduiIconButton -> SduiIconButtonComponent(component, modifier, onAction)
        else -> {
            Box(
                modifier = modifier
                    .padding(8.dp)
                    .background(Red.copy(alpha = 0.1f))
            ) {
                Text(
                    text = "Unhandled Component: ${component::class.simpleName}",
                    color = Red,
                    style = typography.bodySmall
                )
            }
        }
    }
}


