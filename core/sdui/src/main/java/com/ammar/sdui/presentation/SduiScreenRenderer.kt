package com.ammar.sdui.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.ammar.sdui.domain.model.SduiScaffold
import io.github.ammaralghifary.blueprint.Blueprint
import io.github.ammaralghifary.blueprint.model.SduiAction
import io.github.ammaralghifary.blueprint.model.SduiComponent
import io.github.ammaralghifary.blueprint.model.SduiScreen
import io.github.ammaralghifary.blueprint.render.BlueprintRenderer

/**
 * The Blueprint that renders SDUI. It has to be the instance that decoded the screen, so the app root
 * provides the injected one. There is deliberately no default: a second instance could drift apart.
 */
val LocalSduiBlueprint = staticCompositionLocalOf<Blueprint> {
    error("No Blueprint provided. Wrap the UI in CompositionLocalProvider(LocalSduiBlueprint provides blueprint).")
}

/** Renders a decoded screen with [LocalSduiBlueprint]. Features call this instead of Blueprint directly. */
@Composable
fun SduiScreenRenderer(
    screen: SduiScreen,
    modifier: Modifier = Modifier,
    onAction: (SduiAction) -> Unit = {}
) {
    BlueprintRenderer(screen, LocalSduiBlueprint.current, modifier, onAction = onAction)
}

/** Renders one subtree, e.g. a screen body without its scaffold. */
@Composable
fun SduiScreenRenderer(
    component: SduiComponent,
    modifier: Modifier = Modifier,
    onAction: (SduiAction) -> Unit = {}
) {
    BlueprintRenderer(component, LocalSduiBlueprint.current, modifier, onAction = onAction)
}

/** The screen's body without the toolbar scaffold, for hosts that draw their own chrome (e.g. search). */
val SduiScreen.content: SduiComponent
    get() = (body as? SduiScaffold)?.body ?: body
