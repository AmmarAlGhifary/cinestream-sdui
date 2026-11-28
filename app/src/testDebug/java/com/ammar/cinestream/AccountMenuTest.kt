package com.ammar.cinestream

import android.app.Application
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.ammar.home.ui.HomeScreen
import com.ammar.home.viewmodel.HomeViewModel
import com.ammar.network.error.AppError
import com.ammar.network.session.Account
import com.ammar.network.session.SessionState
import com.ammar.sdui.domain.usecase.GetSduiScreenUseCase
import com.ammar.sdui.presentation.LocalSduiBlueprint
import com.ammar.sdui.presentation.registry.CineStreamBlueprint
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** The account menu the app adds to the server's home toolbar. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class AccountMenuTest {

    @get:Rule
    val compose = createComposeRule()

    private val repository = FakeRepository(
        screen(
            """{"type":"column","children":[{"type":"text","text_content":"home content"}]}""",
            toolbar = """{"type":"app_bar","title":{"type":"toolbar_title","text_content":"CineStream"},
                "actions":[{"type":"icon_button","icon_name":"search"}]}"""
        )
    )
    private val sessions = FakeSessions()
    private val signedIn = SessionState.SignedIn(Account("7", "tester"))

    private fun open() {
        val viewModel = HomeViewModel(GetSduiScreenUseCase(repository), sessions)
        compose.setContent {
            CompositionLocalProvider(LocalSduiBlueprint provides CineStreamBlueprint.instance) {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToDetail = {},
                    onNavigateToList = {},
                    onNavigateToSearch = {},
                    onSignIn = {}
                )
            }
        }
    }

    private fun icon() = compose.onNodeWithContentDescription("Account")

    private fun tapIcon() {
        icon().performClick()
        compose.waitForIdle()
    }

    @Test
    fun `signed out there is no account icon`() {
        open()

        compose.onNodeWithContentDescription("search").assertIsDisplayed()
        icon().assertDoesNotExist()
    }

    @Test
    fun `the icon follows the session`() {
        open()

        compose.runOnIdle { sessions.session.value = signedIn }
        icon().assertIsDisplayed()
        compose.onNodeWithContentDescription("search").assertIsDisplayed()

        compose.runOnIdle { sessions.session.value = SessionState.SignedOut }
        icon().assertDoesNotExist()
    }

    @Test
    fun `the menu names the account, and the name is not a button`() {
        sessions.session.value = signedIn
        open()
        compose.onNodeWithText("Sign out").assertDoesNotExist()

        tapIcon()

        compose.onNodeWithText("Signed in as tester").assertIsDisplayed().assertHasNoClickAction()
        compose.onNodeWithText("Sign out").assertIsDisplayed()
    }

    @Test
    fun `sign out ends the session and loads the screen again`() {
        sessions.session.value = signedIn
        open()
        tapIcon()

        compose.onNodeWithText("Sign out").performClick()
        compose.waitForIdle()

        assertEquals(1, sessions.signOuts)
        assertEquals(SessionState.SignedOut, sessions.session.value)
        assertEquals(2, repository.requests.size)
        icon().assertDoesNotExist()
        compose.onNodeWithText("Sign out").assertDoesNotExist()
        compose.onNodeWithText("home content").assertIsDisplayed()
    }

    @Test
    fun `a failed server call signs out without an error dialog`() {
        sessions.session.value = signedIn
        sessions.signOutAnswer = Result.failure(AppError.NoConnection())
        open()
        tapIcon()

        compose.onNodeWithText("Sign out").performClick()
        compose.waitForIdle()

        icon().assertDoesNotExist()
        compose.onNodeWithText("No internet connection").assertDoesNotExist()
    }
}
