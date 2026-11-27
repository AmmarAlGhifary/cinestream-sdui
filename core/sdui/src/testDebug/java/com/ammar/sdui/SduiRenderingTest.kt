package com.ammar.sdui

import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ammar.sdui.presentation.LocalSduiBlueprint
import com.ammar.sdui.presentation.SduiScreenRenderer
import com.ammar.sdui.presentation.registry.CineStreamBlueprint
import io.github.ammaralghifary.blueprint.Blueprint
import io.github.ammaralghifary.blueprint.model.NavigateAction
import io.github.ammaralghifary.blueprint.model.SduiAction
import io.github.ammaralghifary.blueprint.model.SduiComponent
import io.github.ammaralghifary.blueprint.model.SduiModifier
import io.github.ammaralghifary.blueprint.registry.sduiRegistry
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowLog

/** Composes SDUI trees on the JVM. Lives in testDebug because the host activity is a debug dependency. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SduiRenderingTest {

    @get:Rule
    val compose = createComposeRule()

    private val actions = mutableListOf<SduiAction>()

    private fun render(body: String, blueprint: Blueprint = CineStreamBlueprint.instance) {
        val screen = blueprint.decodeScreen("""{"screen_id":"x","body":$body}""")
        compose.setContent {
            CompositionLocalProvider(LocalSduiBlueprint provides blueprint) {
                SduiScreenRenderer(screen, onAction = { actions += it })
            }
        }
    }

    private fun text(content: String) = """{"type":"text","text_content":"$content"}"""

    private fun column(vararg children: String) = """{"type":"column","children":[${children.joinToString()}]}"""

    private fun list(vararg items: String) = """{"type":"vertical_list","items":[${items.joinToString()}]}"""

    @Test
    fun `a vertical list inside a column renders along with its siblings`() {
        render(column(text("above"), list(text("first"), text("second")), text("below")))

        for (label in listOf("above", "first", "second", "below")) {
            compose.onNodeWithText(label).assertIsDisplayed()
        }
    }

    @Test
    fun `a column nested in a column renders`() {
        render(column(column(text("inner")), text("outer")))

        compose.onNodeWithText("inner").assertIsDisplayed()
        compose.onNodeWithText("outer").assertIsDisplayed()
    }

    @Test
    fun `a column or list inside a vertical list item renders`() {
        render(list(column(text("in column")), list(text("in list"))))

        compose.onNodeWithText("in column").assertIsDisplayed()
        compose.onNodeWithText("in list").assertIsDisplayed()
    }

    @Test
    fun `tapping an icon button dispatches without logging the action`() {
        render(
            """{"type":"icon_button","icon_name":"search",
                "action":{"type":"navigation_action","destination":"d","params":{"movie_id":"secret-id"}}}"""
        )

        compose.onNodeWithContentDescription("search").performClick()
        compose.waitForIdle()

        assertEquals("d", (actions.single() as NavigateAction).destination)
        assertTrue(ShadowLog.getLogs().none { "secret-id" in it.msg })
    }

    @Test
    fun `decode problems are logged without server supplied type names`() {
        CineStreamBlueprint.instance.decodeScreen("""{"screen_id":"x","body":{"type":"server-made-up-type"}}""")

        val logged = ShadowLog.getLogsForTag("Blueprint").map { it.msg }
        assertTrue(logged.any { "UnregisteredType" in it })
        assertTrue(logged.none { "server-made-up-type" in it })
    }

    @Serializable
    data class Probe(override val modifier: SduiModifier? = null) : SduiComponent()

    @Test
    fun `the renderer uses the provided Blueprint`() {
        val other = Blueprint(
            sduiRegistry { component("probe", Probe::class, Probe.serializer()) { _, _, _ -> Text("from the provided instance") } }
        )

        render("""{"type":"probe"}""", other)

        compose.onNodeWithText("from the provided instance").assertIsDisplayed()
    }
}
