package com.ammar.cinestream

import android.app.Application
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.lifecycle.SavedStateHandle
import com.ammar.detail.ui.DetailScreen
import com.ammar.detail.viewmodel.DetailViewModel
import com.ammar.home.ui.HomeScreen
import com.ammar.home.viewmodel.HomeViewModel
import com.ammar.listmovie.ui.ListMovieScreen
import com.ammar.listmovie.viewmodel.ListMovieViewModel
import com.ammar.network.error.AppError
import com.ammar.sdui.domain.usecase.GetSduiScreenUseCase
import com.ammar.sdui.domain.usecase.SendApiActionUseCase
import com.ammar.sdui.presentation.LocalSduiBlueprint
import com.ammar.sdui.presentation.registry.CineStreamBlueprint
import com.ammar.search.ui.SearchScreen
import com.ammar.search.viewmodel.SearchViewModel
import io.github.ammaralghifary.blueprint.model.SduiScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import java.time.Duration

/** The real screens and ViewModels, with only the repository faked. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = Application::class)
class ErrorDialogScreensTest {

    @get:Rule
    val compose = createComposeRule()

    private val content = screen(
        """{"type":"column","children":[{"type":"text","text_content":"loaded content"}]}"""
    )

    private fun failed(error: AppError): Result<SduiScreen> = Result.failure(error)

    private val offline = failed(AppError.NoConnection())

    private var backs = 0

    private val sessions = FakeSessions()

    private fun setScreen(screen: @Composable () -> Unit) {
        compose.setContent {
            CompositionLocalProvider(LocalSduiBlueprint provides CineStreamBlueprint.instance, content = screen)
        }
    }

    private fun home(repository: FakeRepository): HomeViewModel {
        val viewModel = HomeViewModel(GetSduiScreenUseCase(repository), sessions)
        setScreen {
            HomeScreen(
                viewModel = viewModel,
                onNavigateToDetail = {},
                onNavigateToList = {},
                onNavigateToSearch = {},
                onSignIn = {}
            )
        }
        return viewModel
    }

    private fun detail(repository: FakeRepository, movieId: String? = "42"): DetailViewModel {
        val viewModel = DetailViewModel(
            GetSduiScreenUseCase(repository),
            SendApiActionUseCase(FakeActions()),
            sessions,
            SavedStateHandle(buildMap { movieId?.let { put("movie_id", it) } })
        )
        setScreen {
            DetailScreen(
                viewModel = viewModel,
                onNavigateBack = { backs++ },
                onSignIn = {},
                onNavigateToDetail = {},
                onNavigateToList = { _, _ -> }
            )
        }
        return viewModel
    }

    private fun list(repository: FakeRepository): ListMovieViewModel {
        val viewModel = ListMovieViewModel(
            GetSduiScreenUseCase(repository),
            SavedStateHandle(mapOf("list_type" to "trending")),
            sessions
        )
        setScreen {
            ListMovieScreen(
                viewModel = viewModel,
                onNavigateBack = { backs++ },
                onSignIn = {},
                onNavigateToDetail = {}
            )
        }
        return viewModel
    }

    private fun search(repository: FakeRepository): SearchViewModel {
        val viewModel = SearchViewModel(GetSduiScreenUseCase(repository), sessions)
        setScreen { SearchScreen(viewModel = viewModel, onNavigateToDetail = {}, onSignIn = {}) }
        return viewModel
    }

    private fun type(viewModel: SearchViewModel, query: String) {
        compose.runOnIdle { viewModel.onQueryChanged(query) }
        shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(600))
        compose.waitForIdle()
    }

    private fun text(value: String) = compose.onNodeWithText(value)

    private fun tap(value: String) {
        text(value).performClick()
        compose.waitForIdle()
    }

    private fun pullToRefresh() {
        compose.onRoot().performTouchInput { swipeDown() }
        compose.waitForIdle()
    }

    private fun assertNoDialog() = text("No internet connection").assertDoesNotExist()

    @Test
    fun `home shows the dialog and retry reloads`() {
        val repository = FakeRepository(offline)
        home(repository)
        text("No internet connection").assertIsDisplayed()

        repository.next = content
        tap("Retry")

        text("loaded content").assertIsDisplayed()
        assertNoDialog()
        assertEquals(2, repository.requests.size)
    }

    @Test
    fun `home still offers retry after the dialog is closed`() {
        val repository = FakeRepository(offline)
        home(repository)
        tap("Close")
        assertNoDialog()

        repository.next = content
        tap("Retry")

        text("loaded content").assertIsDisplayed()
        assertEquals(2, repository.requests.size)
    }

    @Test
    fun `home can be pulled to refresh after the dialog is closed`() {
        val repository = FakeRepository(offline)
        home(repository)
        tap("Close")

        repository.next = content
        pullToRefresh()

        text("loaded content").assertIsDisplayed()
        assertEquals(2, repository.requests.size)
    }

    @Test
    fun `a failed refresh keeps the home content and retry refreshes it`() {
        val repository = FakeRepository(content)
        val viewModel = home(repository)

        repository.next = offline
        pullToRefresh()
        text("No internet connection").assertIsDisplayed()
        text("loaded content").assertExists()

        tap("Retry")
        text("No internet connection").assertIsDisplayed()
        text("loaded content").assertExists()

        tap("Close")
        assertNoDialog()
        text("loaded content").assertIsDisplayed()
        assertEquals(3, repository.requests.size)
        assertEquals(false, viewModel.isRefreshing.value)
    }

    @Test
    fun `detail retry reloads the same movie`() {
        val repository = FakeRepository(offline)
        detail(repository)

        repository.next = content
        tap("Retry")

        text("loaded content").assertIsDisplayed()
        assertEquals(List(2) { mapOf("movie_id" to "42") }, repository.requests)
        assertEquals(0, backs)
    }

    @Test
    fun `detail go back leaves the screen once`() {
        detail(FakeRepository(offline))

        tap("Go back")

        assertEquals(1, backs)
        assertNoDialog()
    }

    @Test
    fun `detail without a movie id cannot be retried`() {
        val repository = FakeRepository(content)
        detail(repository, movieId = null)

        text("Can't open this page").assertIsDisplayed()
        text("Retry").assertDoesNotExist()
        text("Go back").assertIsDisplayed()
        assertEquals(0, repository.requests.size)
    }

    @Test
    fun `a failed refresh keeps the detail content and close stays on the page`() {
        val repository = FakeRepository(content)
        val viewModel = detail(repository)

        repository.next = offline
        compose.runOnIdle { viewModel.refreshScreen() }
        text("loaded content").assertExists()
        tap("Close")

        text("loaded content").assertIsDisplayed()
        assertEquals(0, backs)
    }

    @Test
    fun `a movie that disappeared on refresh leads back`() {
        val repository = FakeRepository(content)
        val viewModel = detail(repository)

        repository.next = failed(AppError.Http(404, serverCode = "MOVIE_NOT_FOUND"))
        compose.runOnIdle { viewModel.refreshScreen() }
        text("Close").assertDoesNotExist()
        tap("Go back")

        assertEquals(1, backs)
    }

    @Test
    fun `list shows the dialog, retries and goes back`() {
        val repository = FakeRepository(offline)
        list(repository)

        tap("Retry")
        assertEquals(List(2) { mapOf("list_type" to "trending") }, repository.requests)
        tap("Go back")

        assertEquals(1, backs)
    }

    @Test
    fun `a failed refresh keeps the list content`() {
        val repository = FakeRepository(content)
        val viewModel = list(repository)

        repository.next = offline
        compose.runOnIdle { viewModel.refreshScreen() }
        text("No internet connection").assertIsDisplayed()
        text("loaded content").assertExists()

        repository.next = content
        tap("Retry")
        assertNoDialog()
        text("loaded content").assertIsDisplayed()
    }

    @Test
    fun `search shows the dialog and retry runs the query again`() {
        val repository = FakeRepository(offline)
        val viewModel = search(repository)
        type(viewModel, "dune")
        text("No internet connection").assertIsDisplayed()

        repository.next = content
        tap("Retry")

        text("loaded content").assertIsDisplayed()
        assertEquals(List(2) { mapOf("query" to "dune") }, repository.requests)
    }

    @Test
    fun `search still offers retry after the dialog is closed`() {
        val repository = FakeRepository(offline)
        val viewModel = search(repository)
        type(viewModel, "dune")
        tap("Close")
        assertNoDialog()

        repository.next = content
        tap("Retry")

        text("loaded content").assertIsDisplayed()
    }

    @Test
    fun `an empty query and an empty result are normal screens`() {
        val repository = FakeRepository(screen("""{"type":"column","children":[]}"""))
        val viewModel = search(repository)

        type(viewModel, "   ")
        text("Type a movie name to search...").assertIsDisplayed()
        assertEquals(0, repository.requests.size)

        type(viewModel, "nothing matches")
        assertEquals(1, repository.requests.size)
        text("Close").assertDoesNotExist()
        text("Retry").assertDoesNotExist()
    }

    @Test
    fun `every screen loads again after a sign-in`() {
        val repository = FakeRepository(content)
        home(repository)

        compose.runOnIdle { sessions.accountChanges.tryEmit(Unit) }
        compose.waitForIdle()

        assertEquals(2, repository.requests.size)
        text("loaded content").assertIsDisplayed()
    }

    @Test
    fun `the search query is capped at 200 characters without splitting an emoji`() {
        val repository = FakeRepository(content)
        val viewModel = search(repository)

        type(viewModel, "a".repeat(250))
        assertEquals("a".repeat(200), viewModel.searchQuery.value)
        assertEquals("a".repeat(200), repository.requests.single()["query"])

        // Typing past the limit changes nothing, so it does not search again.
        type(viewModel, "a".repeat(201))
        assertEquals(1, repository.requests.size)

        type(viewModel, "b".repeat(199) + "🎬")
        assertEquals("b".repeat(199), viewModel.searchQuery.value)
    }
}
