package com.ammar.network

import com.ammar.network.error.AppError
import com.ammar.network.session.Account
import com.ammar.network.session.SessionRepositoryImpl
import com.ammar.network.session.SessionState
import com.ammar.network.session.SessionStore
import com.ammar.network.session.SignInCallback
import com.ammar.network.session.parseExpiry
import com.ammar.network.source.AuthApiService
import com.ammar.network.source.AuthInterceptor
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Retrofit
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

class SessionRepositoryTest {

    private val server = MockWebServer().apply { start() }
    private val store = SessionStore(storage())
    private var now: Instant = Instant.parse("2026-11-27T10:00:00Z")

    private val clock = object : Clock() {
        override fun instant() = now
        override fun getZone() = ZoneOffset.UTC
        override fun withZone(zone: java.time.ZoneId) = this
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(store, server.url("/")))
        .readTimeout(1, TimeUnit.SECONDS)
        .build()

    private val repository = SessionRepositoryImpl(
        Retrofit.Builder().baseUrl(server.url("/")).client(client).build().create(AuthApiService::class.java),
        store,
        clock
    )

    @After
    fun tearDown() = server.shutdown()

    private fun json(body: String, status: Int = 200) =
        MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json").setBody(body)

    private fun tokenAnswer(
        expiresAt: String = "2026-11-27 10:15:00 UTC",
        approveUrl: String = "https://www.themoviedb.org/authenticate/test-token"
    ) = json("""{"request_token":"test-token","expires_at":"$expiresAt","approve_url":"$approveUrl"}""")

    private val sessionAnswer =
        json("""{"session_id":"test-session","account":{"id":7,"username":"tester"}}""")

    private val approved = "cinestream://auth/tmdb?request_token=test-token&approved=true"

    private fun start(answer: MockResponse = tokenAnswer()): Result<String> = runBlocking {
        server.enqueue(answer)
        repository.startSignIn()
    }

    private fun complete(link: String = approved): Result<Account> =
        runBlocking { repository.completeSignIn(link) }

    private fun assertRejected(result: Result<Account>) {
        assertTrue(result.exceptionOrNull().toString(), result.exceptionOrNull() is AppError.SignInFailed)
        assertEquals(SessionState.SignedOut, repository.session.value)
    }

    @Test
    fun `starting posts without a body and returns the approve page`() {
        val url = start().getOrThrow()

        assertEquals("https://www.themoviedb.org/authenticate/test-token", url)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/auth/request_token", request.path)
        assertEquals(0, request.bodySize)
    }

    @Test
    fun `an approved link creates and stores the session`() = runBlocking {
        start()
        server.takeRequest()
        server.enqueue(sessionAnswer)
        val signedIn = async(start = CoroutineStart.UNDISPATCHED) { repository.signIns.first() }

        val account = repository.completeSignIn(approved).getOrThrow()

        assertEquals(Account("7", "tester"), account)
        assertEquals(SessionState.SignedIn(account), repository.session.value)
        assertEquals("test-session", store.sessionId())
        assertNull(store.pending())
        withTimeout(1000) { signedIn.await() }
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/auth/session", request.path)
        assertEquals("""{"request_token":"test-token"}""", request.body.readUtf8())
        assertNull(request.getHeader("Authorization"))
    }

    @Test
    fun `a declined link is rejected without a request`() {
        start()

        assertRejected(complete("cinestream://auth/tmdb?request_token=test-token&approved=false"))
        assertEquals(1, server.requestCount)
        assertNull(store.pending())
    }

    @Test
    fun `a link for another token is rejected and the real sign-in can still finish`() {
        start()

        assertRejected(complete("cinestream://auth/tmdb?request_token=someone-else&approved=true"))
        assertEquals(1, server.requestCount)

        server.enqueue(sessionAnswer)
        assertTrue(complete().isSuccess)
    }

    @Test
    fun `links without exactly one token and one approval are rejected`() {
        start()
        for (link in listOf(
            "cinestream://auth/tmdb",
            "cinestream://auth/tmdb?approved=true",
            "cinestream://auth/tmdb?request_token=&approved=true",
            "cinestream://auth/tmdb?request_token=test-token",
            "cinestream://auth/tmdb?request_token=test-token&approved=TRUE",
            "cinestream://auth/tmdb?request_token=test-token&request_token=test-token&approved=true",
            "cinestream://auth/tmdb?request_token=test-token&approved=false&approved=true",
            "https://auth/tmdb?request_token=test-token&approved=true",
            "cinestream://auth/other?request_token=test-token&approved=true",
            "cinestream://evil/tmdb?request_token=test-token&approved=true",
            "not a link"
        )) {
            // A declined link spends the token, so each case starts over.
            if (store.pending() == null) start()
            assertRejected(complete(link))
        }
        assertTrue(server.requestCount >= 1)
        generateSequence { server.takeRequest(0, TimeUnit.SECONDS) }
            .forEach { assertEquals("/api/auth/request_token", it.path) }
    }

    @Test
    fun `only the sign-in link is recognised`() {
        assertNotNull(SignInCallback.parse(approved))
        assertNull(SignInCallback.parse("cinestream://other/tmdb"))
        assertNull(SignInCallback.parse(null))
    }

    @Test
    fun `a link without a sign-in in progress is rejected`() {
        assertRejected(complete())
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `an expired token is rejected`() {
        start()
        now = Instant.parse("2026-11-27T10:15:00Z")

        assertRejected(complete())
        assertEquals(1, server.requestCount)
        assertNull(store.pending())
    }

    @Test
    fun `an unreadable expiry falls back to fifteen minutes`() {
        start(tokenAnswer(expiresAt = "soon"))

        now = now.plusSeconds(15 * 60)

        assertRejected(complete())
    }

    @Test
    fun `expiry is read as ISO 8601 or in TMDB's form`() {
        val expected = Instant.parse("2026-11-27T10:15:00Z")
        assertEquals(expected, parseExpiry("2026-11-27 10:15:00 UTC"))
        assertEquals(expected, parseExpiry("2026-11-27T10:15:00Z"))
        assertEquals(expected, parseExpiry("2026-11-27T17:15:00+07:00"))
        assertNull(parseExpiry("soon"))
        assertNull(parseExpiry(null))
    }

    @Test
    fun `only a secure approve page is accepted`() {
        for (url in listOf("http://www.themoviedb.org/x", "intent://x#Intent;end", "javascript:alert(1)", "")) {
            assertTrue(url, start(tokenAnswer(approveUrl = url)).exceptionOrNull() is AppError.Parse)
            assertNull(store.pending())
        }
    }

    @Test
    fun `a server refusal is typed and spends the token`() {
        start()
        server.enqueue(json("""{"error":{"code":"SIGN_IN_FAILED","retryable":false}}""", 401))

        val error = complete().exceptionOrNull() as AppError.Http

        assertEquals("SIGN_IN_FAILED", error.serverCode)
        assertNull(store.pending())
    }

    @Test
    fun `after a server or connection failure the same link can be tried again`() {
        start()
        server.enqueue(json("{}", 503))
        assertTrue(complete().exceptionOrNull() is AppError.Http)
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
        assertTrue(complete().exceptionOrNull() is AppError.Timeout)

        server.enqueue(sessionAnswer)

        assertTrue(complete().isSuccess)
    }

    @Test
    fun `an unreadable session answer is a parse error that quotes nothing`() {
        start()
        for (body in listOf("""{"session_id":"test-session"}""", """{"session_id":"has space","account":{"id":1}}""")) {
            server.enqueue(json(body))
            val error = complete().exceptionOrNull() as AppError.Parse
            assertNull(error.cause)
            assertEquals(SessionState.SignedOut, repository.session.value)
        }
    }

    private fun signIn() {
        start()
        server.enqueue(sessionAnswer)
        complete().getOrThrow()
        repeat(2) { server.takeRequest() }
    }

    @Test
    fun `sign out deletes the session on the server and forgets it`() = runBlocking {
        signIn()
        server.enqueue(MockResponse().setResponseCode(204))

        assertTrue(repository.signOut().isSuccess)

        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/auth/session", request.path)
        assertEquals("Bearer test-session", request.getHeader("Authorization"))
        assertEquals(SessionState.SignedOut, repository.session.value)
    }

    @Test
    fun `sign out forgets the session even when the server call fails`() = runBlocking {
        signIn()
        server.enqueue(json("{}", 500))

        assertTrue(repository.signOut().exceptionOrNull() is AppError.Http)

        assertEquals(SessionState.SignedOut, repository.session.value)
    }

    @Test
    fun `signing out while signed out calls nothing, and clear forgets everything`() = runBlocking {
        assertTrue(repository.signOut().isSuccess)
        assertEquals(0, server.requestCount)

        signIn()
        start()
        repository.clear()

        assertEquals(SessionState.SignedOut, repository.session.value)
        assertNull(store.pending())
    }
}
