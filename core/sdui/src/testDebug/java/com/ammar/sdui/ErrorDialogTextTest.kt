package com.ammar.sdui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ammar.designsystems.ErrorDialog
import com.ammar.designsystems.ErrorDialogModel
import com.ammar.designsystems.ErrorPrimaryAction
import com.ammar.designsystems.ErrorSecondaryAction
import com.ammar.network.error.AppError
import com.ammar.sdui.presentation.error.errorDialogModel
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.net.UnknownHostException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ErrorDialogTextTest {

    @get:Rule
    val compose = createComposeRule()

    private var retries = 0
    private var dismissals = 0
    private var signIns = 0
    private var model: ErrorDialogModel? = null

    private fun show(
        error: AppError,
        debug: Boolean = false,
        secondary: ErrorSecondaryAction = ErrorSecondaryAction.CLOSE
    ) {
        compose.setContent {
            val shown = errorDialogModel(error, secondary, showTechnicalDetail = debug)
            model = shown
            ErrorDialog(
                model = shown,
                onRetry = { retries++ },
                onDismiss = { dismissals++ },
                onSignIn = { signIns++ }
            )
        }
    }

    private val offline = AppError.NoConnection(UnknownHostException("Unable to resolve host \"secret.host\""))

    @Test
    fun `a release dialog shows readable text only and both buttons work`() {
        show(offline)

        compose.onNodeWithText("No internet connection").assertIsDisplayed()
        compose.onNodeWithText("Check your connection and try again.").assertIsDisplayed()
        for (technical in listOf("secret.host", "Exception", "NoConnection")) {
            compose.onNodeWithText(technical, substring = true).assertDoesNotExist()
        }

        compose.onNodeWithText("Retry").performClick()
        compose.onNodeWithText("Close").performClick()
        assertEquals(1, retries)
        assertEquals(1, dismissals)
    }

    @Test
    fun `a debug dialog adds the technical line without the host`() {
        show(offline, debug = true)

        compose.onNodeWithText("NoConnection · UnknownHostException").assertIsDisplayed()
        compose.onNodeWithText("secret.host", substring = true).assertDoesNotExist()
    }

    @Test
    fun `an error that cannot be retried has only the secondary button`() {
        show(AppError.Http(404, serverCode = "MOVIE_NOT_FOUND", requestId = "req-1"), secondary = ErrorSecondaryAction.GO_BACK)

        compose.onNodeWithText("Movie not found").assertIsDisplayed()
        compose.onNodeWithText("We couldn't find this movie. It may have been removed.").assertIsDisplayed()
        compose.onNodeWithText("Retry").assertDoesNotExist()
        compose.onNodeWithText("req-1", substring = true).assertDoesNotExist()
        compose.onNodeWithText("Go back").performClick()
        assertEquals(1, dismissals)
    }

    @Test
    fun `retry after is spelled out under the message`() {
        show(AppError.Http(503, serverCode = "CONTENT_UNAVAILABLE", retryAfterSeconds = 30))

        compose.onNodeWithText("We couldn't load movies right now. Please try again in a moment.").assertIsDisplayed()
        compose.onNodeWithText("Try again in 30 seconds.").assertIsDisplayed()
    }

    @Test
    fun `waits under a minute are in seconds and longer ones in minutes rounded up`() {
        var wait by mutableStateOf(1)
        compose.setContent {
            ErrorDialog(
                model = errorDialogModel(AppError.Http(503, serverCode = "MAINTENANCE", retryAfterSeconds = wait)),
                onRetry = {},
                onDismiss = {},
                onSignIn = {}
            )
        }
        val expected = listOf(
            1 to "1 second",
            59 to "59 seconds",
            60 to "1 minute",
            61 to "2 minutes",
            120 to "2 minutes",
            300 to "5 minutes",
            86_400 to "1440 minutes"
        )

        for ((seconds, text) in expected) {
            compose.runOnIdle { wait = seconds }
            compose.onNodeWithText("Try again in $text.").assertIsDisplayed()
        }
    }

    private fun assertSignInDialog(error: AppError, title: String, message: String) {
        // The screen asks for Go back, but a sign-in error is always closed instead.
        show(error, secondary = ErrorSecondaryAction.GO_BACK)

        assertEquals(ErrorPrimaryAction.SIGN_IN, model?.primaryAction)
        assertEquals(ErrorSecondaryAction.CLOSE, model?.secondaryAction)
        compose.onNodeWithText(title).assertIsDisplayed()
        compose.onNodeWithText(message).assertIsDisplayed()
        compose.onNodeWithText("Retry").assertDoesNotExist()
        compose.onNodeWithText("Go back").assertDoesNotExist()
        compose.onNodeWithText("Sign in").performClick()
        compose.onNodeWithText("Close").performClick()
        assertEquals(listOf(1, 1, 0), listOf(signIns, dismissals, retries))
    }

    @Test
    fun `sign in required offers sign in`() = assertSignInDialog(
        AppError.Http(401, serverCode = "SIGN_IN_REQUIRED"),
        "Sign in required",
        "Sign in with your TMDB account to save favorites."
    )

    @Test
    fun `an expired session offers sign in`() = assertSignInDialog(
        AppError.Http(401, serverCode = "SESSION_EXPIRED"),
        "Session expired",
        "Please sign in again to keep using favorites."
    )

    @Test
    fun `a failed sign-in from the server offers sign in`() = assertSignInDialog(
        AppError.Http(401, serverCode = "SIGN_IN_FAILED"),
        "Sign-in didn't finish",
        "We couldn't complete sign-in. Please try again."
    )

    @Test
    fun `a rejected sign-in link offers sign in`() = assertSignInDialog(
        AppError.SignInFailed(),
        "Sign-in didn't finish",
        "We couldn't complete sign-in. Please try again."
    )

    @Test
    fun `server title and message replace the fallback`() {
        show(AppError.Http(503, "MAINTENANCE", serverTitle = "Back soon", serverMessage = "Polishing things."))

        compose.onNodeWithText("Back soon").assertIsDisplayed()
        compose.onNodeWithText("Polishing things.").assertIsDisplayed()
        compose.onNodeWithText("Under maintenance").assertDoesNotExist()
    }
}
