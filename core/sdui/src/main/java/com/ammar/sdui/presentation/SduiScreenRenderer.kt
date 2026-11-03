package com.ammar.sdui.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.ammar.sdui.domain.model.SduiScaffold
import com.ammar.sdui.presentation.registry.CineStreamBlueprint
import io.github.ammaralghifary.blueprint.model.SduiAction
import io.github.ammaralghifary.blueprint.model.SduiComponent
import io.github.ammaralghifary.blueprint.model.SduiScreen
import io.github.ammaralghifary.blueprint.render.BlueprintRenderer

/** Renders a decoded screen with CineStream's registry. Features call this instead of Blueprint directly. */
@Composable
fun SduiScreenRenderer(
    screen: SduiScreen,
    modifier: Modifier = Modifier,
    onAction: (SduiAction) -> Unit = {}
) {
    BlueprintRenderer(screen, CineStreamBlueprint.instance, modifier, onAction = onAction)
}

/** Renders one subtree, e.g. a screen body without its scaffold. */
@Composable
fun SduiScreenRenderer(
    component: SduiComponent,
    modifier: Modifier = Modifier,
    onAction: (SduiAction) -> Unit = {}
) {
    BlueprintRenderer(component, CineStreamBlueprint.instance, modifier, onAction = onAction)
}

/** The screen's body without the toolbar scaffold, for hosts that draw their own chrome (e.g. search). */
val SduiScreen.content: SduiComponent
    get() = (body as? SduiScaffold)?.body ?: body
