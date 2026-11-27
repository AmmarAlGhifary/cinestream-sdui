package com.ammar.cinestream

import android.app.Application
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ActivityInfo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.net.toUri
import androidx.test.core.app.ApplicationProvider
import com.ammar.cinestream.auth.SignInHost
import com.ammar.cinestream.auth.SignInViewModel
import com.ammar.network.error.AppError
import com.ammar.network.session.SignInOutcome
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class SignInTest {

    @get:Rule
    val compose = createComposeRule()

    private val sessions = FakeSessions()
    private val viewModel = SignInViewModel(sessions)
    private val opened = mutableListOf<String>()
    private var hasBrowser = true

    private val link = "cinestream://auth/tmdb?request_token=test-token&approved=true"

    private fun host() = compose.setContent {
        SignInHost(viewModel, openPage = { opened += it; hasBrowser })
    }

    private fun run(block: () -> Unit) {
        compose.runOnIdle(block)
        compose.waitForIdle()
    }

    private fun tap(text: String) {
        compose.onNodeWithText(text).performClick()
        compose.waitForIdle()
    }

    @Test
    fun `signing in opens the approval page once`() {
        host()

        run(viewModel::signIn)

        assertEquals(listOf("https://tmdb.example/approve"), opened)
    }

    @Test
    fun `a failed start shows a dialog and retry starts again`() {
        sessions.start = Result.failure(AppError.NoConnection())
        host()

        run(viewModel::signIn)
        compose.onNodeWithText("No internet connection").assertIsDisplayed()
        sessions.start = Result.success("https://tmdb.example/approve")
        tap("Retry")

        assertEquals(2, sessions.starts)
        assertEquals(1, opened.size)
        compose.onNodeWithText("No internet connection").assertDoesNotExist()
    }

    @Test
    fun `a rejected link shows the sign-in failure and offers to start over`() {
        sessions.complete = Result.failure(AppError.SignInFailed())
        host()

        run { viewModel.onLink(link) }
        compose.onNodeWithText("Sign-in didn't finish").assertIsDisplayed()
        tap("Sign in")

        assertEquals(1, sessions.starts)
        compose.onNodeWithText("Sign-in didn't finish").assertDoesNotExist()
    }

    @Test
    fun `retry after a connection failure uses the same link`() {
        sessions.complete = Result.failure(AppError.Timeout())
        host()

        run { viewModel.onLink(link) }
        tap("Retry")

        assertEquals(listOf(link, link), sessions.links)
        assertEquals(0, sessions.starts)
    }

    @Test
    fun `an ignored link shows nothing`() {
        sessions.complete = Result.success(SignInOutcome.Ignored)
        host()

        run { viewModel.onLink(link) }

        assertEquals(listOf(link), sessions.links)
        compose.onNodeWithText("Sign-in didn't finish").assertDoesNotExist()
        compose.onNodeWithText("Close").assertDoesNotExist()
    }

    @Test
    fun `an ignored link leaves an earlier error on screen`() {
        sessions.complete = Result.failure(AppError.SignInFailed())
        host()
        run { viewModel.onLink(link) }

        sessions.complete = Result.success(SignInOutcome.Ignored)
        run { viewModel.onLink("cinestream://auth/tmdb?request_token=forged&approved=true") }

        compose.onNodeWithText("Sign-in didn't finish").assertIsDisplayed()
    }

    @Test
    fun `other links are ignored`() {
        host()

        run { viewModel.onLink("https://example.org/") }

        assertEquals(emptyList<String>(), sessions.links)
    }

    @Test
    fun `a missing browser is reported`() {
        hasBrowser = false
        host()

        run(viewModel::signIn)

        compose.onNodeWithText("Something went wrong").assertIsDisplayed()
    }

    @Test
    fun `the main activity shares no task affinity`() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val activity = context.packageManager.getActivityInfo(
            ComponentName(context, "com.ammar.cinestream.MainActivity"), 0
        )

        assertTrue(activity.taskAffinity.isNullOrEmpty())
        assertEquals(ActivityInfo.LAUNCH_SINGLE_TASK, activity.launchMode)
    }

    @Test
    fun `the manifest sends the sign-in link to the main activity only`() {
        val packages = ApplicationProvider.getApplicationContext<Application>().packageManager
        fun handlers(url: String) = packages.queryIntentActivities(
            Intent(Intent.ACTION_VIEW, url.toUri()).addCategory(Intent.CATEGORY_BROWSABLE), 0
        ).map { it.activityInfo.name }

        assertEquals(listOf("com.ammar.cinestream.MainActivity"), handlers(link))
        assertEquals(emptyList<String>(), handlers("cinestream://auth/other"))
        assertEquals(emptyList<String>(), handlers("cinestream://other/tmdb"))
    }
}
