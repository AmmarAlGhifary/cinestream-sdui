package com.ammar.cinestream

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.lifecycle.SavedStateHandle
import com.ammar.detail.ui.DetailScreen
import com.ammar.detail.viewmodel.DetailViewModel
import com.ammar.network.error.AppError
import com.ammar.sdui.domain.model.ActionResult
import com.ammar.sdui.domain.model.SduiAppBar
import com.ammar.sdui.domain.model.SduiIconButton
import com.ammar.sdui.domain.model.SduiScaffold
import com.ammar.sdui.domain.usecase.GetSduiScreenUseCase
import com.ammar.sdui.domain.usecase.SendApiActionUseCase
import com.ammar.sdui.presentation.LocalSduiBlueprint
import com.ammar.sdui.presentation.registry.CineStreamBlueprint
import kotlinx.coroutines.CompletableDeferred
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The heart on the detail screen, with the real screen and ViewModel. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class FavoritesTest {

    @get:Rule
    val compose = createComposeRule()

    private fun toolbar(icon: String, next: Boolean) =
        """{"type":"app_bar","title":{"type":"toolbar_title","text_content":"CineStream"},"actions":[
           {"type":"icon_button","icon_name":"$icon","action":{"type":"api_action",
            "endpoint":"/api/favorite","method":"POST","body":{"movie_id":"550","favorite":$next}}}]}"""

    private val lines = (1..60).joinToString { """{"type":"text","text_content":"line $it"}""" }

    private val page = screen("""{"type":"column","children":[$lines]}""", toolbar("favorite_border", true))

    private val filledHeart = ((screen("""{"type":"column","children":[]}""", toolbar("favorite", false))
        .getOrThrow().body as SduiScaffold).toolbar as SduiAppBar).actions.single() as SduiIconButton

    private val repository = FakeRepository(page)
    private val actions = FakeActions { Result.success(ActionResult(filledHeart, true)) }
    private val sessions = FakeSessions()
    private var backs = 0
    private var signInRequests = 0

    private fun open(): DetailViewModel {
        val viewModel = DetailViewModel(
            GetSduiScreenUseCase(repository),
            SendApiActionUseCase(actions),
            sessions,
            SavedStateHandle(mapOf("movie_id" to "550"))
        )
        compose.setContent {
            CompositionLocalProvider(LocalSduiBlueprint provides CineStreamBlueprint.instance) {
                DetailScreen(
                    viewModel = viewModel,
                    onNavigateBack = { backs++ },
                    onSignIn = { signInRequests++ },
                    onNavigateToDetail = {},
                    onNavigateToList = { _, _ -> }
                )
            }
        }
        return viewModel
    }

    private fun tapHeart(icon: String = "favorite_border") {
        compose.onNodeWithContentDescription(icon).performClick()
        compose.waitForIdle()
    }

    private fun tap(text: String) {
        compose.onNodeWithText(text).performClick()
        compose.waitForIdle()
    }

    private fun sentBodies() = actions.sent.map { it.body.toString() }

    private fun failure(code: String, status: Int = 401): suspend () -> Result<ActionResult> =
        { Result.failure(AppError.Http(status, serverCode = code)) }

    @Test
    fun `a tap sends the action and swaps in the returned heart`() {
        open()

        tapHeart()

        compose.onNodeWithContentDescription("favorite").assertIsDisplayed()
        compose.onNodeWithContentDescription("favorite_border").assertDoesNotExist()
        assertEquals(listOf("""{"movie_id":"550","favorite":true}"""), sentBodies())
        assertEquals("the page is not loaded again", 1, repository.requests.size)

        tapHeart("favorite")
        assertEquals("""{"movie_id":"550","favorite":false}""", sentBodies().last())
    }

    @Test
    fun `swapping the heart keeps the scroll position`() {
        open()
        compose.onNodeWithText("line 60").performScrollTo()

        tapHeart()

        compose.onNodeWithText("line 60").assertIsDisplayed()
    }

    @Test
    fun `the favorite state alone redraws the heart`() {
        actions.answer = { Result.success(ActionResult(isFavorite = true)) }
        open()

        tapHeart()

        compose.onNodeWithContentDescription("favorite").assertIsDisplayed()
        assertEquals(1, repository.requests.size)
    }

    @Test
    fun `an answer that cannot be applied loads the page again`() {
        actions.answer = { Result.success(ActionResult()) }
        open()

        tapHeart()

        assertEquals(2, repository.requests.size)
    }

    @Test
    fun `taps are ignored while a request is in flight`() {
        val gate = CompletableDeferred<Result<ActionResult>>()
        actions.answer = { gate.await() }
        open()

        repeat(3) { tapHeart() }
        assertEquals(1, actions.sent.size)

        gate.complete(Result.success(ActionResult(filledHeart, true)))
        compose.waitForIdle()
        compose.onNodeWithContentDescription("favorite").assertIsDisplayed()
        tapHeart("favorite")
        assertEquals(2, actions.sent.size)
    }

    @Test
    fun `a failure shows over the page and retry sends the action again`() {
        actions.answer = { Result.failure(AppError.NoConnection()) }
        open()

        tapHeart()
        compose.onNodeWithText("No internet connection").assertIsDisplayed()
        compose.onNodeWithText("line 1").assertExists()

        actions.answer = { Result.success(ActionResult(filledHeart, true)) }
        tap("Retry")

        compose.onNodeWithText("No internet connection").assertDoesNotExist()
        compose.onNodeWithContentDescription("favorite").assertIsDisplayed()
        assertEquals(2, actions.sent.size)
        assertEquals(1, repository.requests.size)
    }

    @Test
    fun `signed out, the heart offers sign in and the page loads again afterwards`() {
        actions.answer = failure("SIGN_IN_REQUIRED")
        open()

        tapHeart()
        compose.onNodeWithText("Sign in required").assertIsDisplayed()
        tap("Sign in")

        assertEquals(1, signInRequests)
        compose.onNodeWithText("Sign in required").assertDoesNotExist()

        repository.next = screen("""{"type":"column","children":[]}""", toolbar("favorite", false))
        compose.runOnIdle { sessions.accountChanges.tryEmit(Unit) }
        compose.waitForIdle()

        compose.onNodeWithContentDescription("favorite").assertIsDisplayed()
        assertEquals(2, repository.requests.size)
    }

    @Test
    fun `an expired session can be closed without leaving the page`() {
        actions.answer = failure("SESSION_EXPIRED")
        open()

        tapHeart()
        compose.onNodeWithText("Session expired").assertIsDisplayed()
        tap("Close")

        compose.onNodeWithContentDescription("favorite_border").assertIsDisplayed()
        assertEquals(0, backs)
        assertEquals(0, signInRequests)
    }

    @Test
    fun `a movie that is gone leads back`() {
        actions.answer = failure("MOVIE_NOT_FOUND", 404)
        open()

        tapHeart()
        tap("Go back")

        assertEquals(1, backs)
    }

    @Test
    fun `a screen that failed with a sign-in error offers sign in and close`() {
        repository.next = Result.failure(AppError.Http(401, serverCode = "SESSION_EXPIRED"))
        open()

        compose.onNodeWithText("Sign in").assertIsDisplayed()
        tap("Close")

        assertEquals(0, backs)
        compose.onNodeWithText("Retry").assertIsDisplayed()
    }
}
