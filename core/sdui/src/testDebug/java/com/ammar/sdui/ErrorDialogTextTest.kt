package com.ammar.sdui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ammar.designsystems.ErrorDialog
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

    private fun show(
        error: AppError,
        debug: Boolean = false,
        secondary: ErrorSecondaryAction = ErrorSecondaryAction.CLOSE
    ) {
        compose.setContent {
            ErrorDialog(
                model = errorDialogModel(error, secondary, showTechnicalDetail = debug),
                onRetry = { retries++ },
                onDismiss = { dismissals++ }
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
    fun `server title and message replace the fallback`() {
        show(AppError.Http(503, "MAINTENANCE", serverTitle = "Back soon", serverMessage = "Polishing things."))

        compose.onNodeWithText("Back soon").assertIsDisplayed()
        compose.onNodeWithText("Polishing things.").assertIsDisplayed()
        compose.onNodeWithText("Under maintenance").assertDoesNotExist()
    }
}
