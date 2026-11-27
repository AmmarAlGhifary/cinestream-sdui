package com.ammar.network

import com.ammar.network.error.AppError
import com.ammar.network.session.SessionState
import com.ammar.network.source.ActionDataSourceImpl
import com.ammar.network.source.ApiLoggingInterceptor
import com.ammar.network.source.AuthInterceptor
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/** The interceptors and the action data source against real HTTP. */
class AuthNetworkingTest {

    private val backend = MockWebServer().apply { start() }
    private val elsewhere = MockWebServer().apply { start() }
    private val store = signedInStore()
    private val log = mutableListOf<String>()

    private val client = OkHttpClient.Builder()
        .addInterceptor(AuthInterceptor(store, backend.url("/")))
        .addInterceptor(ApiLoggingInterceptor(enabled = true, logger = { log += it }))
        .build()

    private val actions = ActionDataSourceImpl(client, backend.url("/"))

    @After
    fun tearDown() {
        backend.shutdown()
        elsewhere.shutdown()
    }

    /** Sends one request to [server] and returns what arrived. */
    private fun call(
        server: MockWebServer = backend,
        answer: MockResponse = MockResponse(),
        url: HttpUrl = server.url("/api/movie_detail_screen")
    ): RecordedRequest {
        server.enqueue(answer)
        client.newCall(Request.Builder().url(url).build()).execute().use { it.body?.string() }
        return server.takeRequest(1, TimeUnit.SECONDS)!!
    }

    private fun isSignedIn() = store.state.value is SessionState.SignedIn

    private fun expired(code: String, status: Int = 401) =
        MockResponse().setResponseCode(status).setBody("""{"error":{"code":"$code"}}""")

    @Test
    fun `requests to our backend carry the session`() {
        assertEquals("Bearer test-session", call().getHeader("Authorization"))
    }

    @Test
    fun `nothing is added while signed out`() {
        store.clearSession()

        assertNull(call().getHeader("Authorization"))
    }

    @Test
    fun `another port or host never sees the session`() {
        assertNull(call(elsewhere).getHeader("Authorization"))

        val otherName = backend.url("/api/x").newBuilder()
            .host(if (backend.hostName == "localhost") "127.0.0.1" else "localhost")
            .build()
        assertNull(call(url = otherName).getHeader("Authorization"))
    }

    @Test
    fun `a redirect to another origin drops the session and cannot clear it`() {
        elsewhere.enqueue(MockResponse().addHeader("X-Session-Invalid", "true"))
        val redirect = MockResponse().setResponseCode(302).addHeader("Location", elsewhere.url("/there"))

        call(answer = redirect)

        assertNull(elsewhere.takeRequest(1, TimeUnit.SECONDS)!!.getHeader("Authorization"))
        assertTrue(isSignedIn())
    }

    @Test
    fun `the session-invalid header clears the session`() {
        call(answer = MockResponse().setBody("{}").addHeader("X-Session-Invalid", "true"))

        assertFalse(isSignedIn())
    }

    @Test
    fun `a session-expired answer clears the session and keeps its body`() = runBlocking {
        backend.enqueue(expired("SESSION_EXPIRED"))

        val error = actions.send("POST", "/api/favorite", null).exceptionOrNull() as AppError.Http

        assertEquals("SESSION_EXPIRED", error.serverCode)
        assertFalse(isSignedIn())
    }

    @Test
    fun `other answers keep the session`() {
        call(answer = expired("SIGN_IN_REQUIRED"))
        call(answer = MockResponse().setResponseCode(401).setBody("nope"))
        call(answer = expired("INTERNAL_ERROR", 500))
        call(answer = MockResponse().addHeader("X-Session-Invalid", "false"))
        // A successful body that merely mentions the code means nothing.
        call(answer = MockResponse().setBody("""{"error":{"code":"SESSION_EXPIRED"}}"""))

        assertTrue(isSignedIn())
    }

    @Test
    fun `a late answer about an old session keeps the new one`() {
        val slow = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(store, backend.url("/")))
            .addInterceptor { chain ->
                // The user signs in again while this request is still on its way.
                chain.proceed(chain.request()).also {
                    store.save(com.ammar.network.session.StoredSession("newer-session", "7", "tester"))
                }
            }
            .build()
        backend.enqueue(expired("SESSION_EXPIRED"))

        slow.newCall(Request.Builder().url(backend.url("/api/x")).build()).execute().close()

        assertEquals("newer-session", store.sessionId())
    }

    @Test
    fun `the log redacts the session header and keeps ordinary bodies`() {
        call(answer = MockResponse().setBody("""{"screen_id":"visible"}"""))

        assertTrue(log.any { it.startsWith("Authorization:") })
        assertFalse(log.any { "test-session" in it })
        assertTrue(log.any { "visible" in it })
    }

    @Test
    fun `the log has no bodies from the sign-in endpoints`() {
        store.clearSession()
        backend.enqueue(MockResponse().setBody("""{"session_id":"test-session"}"""))
        val request = Request.Builder()
            .url(backend.url("/api/auth/session"))
            .post("""{"request_token":"test-token"}""".toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { assertTrue(it.body!!.string().isNotEmpty()) }

        assertTrue(log.any { "/api/auth/session" in it })
        assertFalse(log.any { "test-session" in it || "test-token" in it })
    }

    @Test
    fun `release logs nothing`() {
        val quiet = OkHttpClient.Builder()
            .addInterceptor(ApiLoggingInterceptor(enabled = false, logger = { log += it }))
            .build()
        backend.enqueue(MockResponse().setBody("{}"))

        quiet.newCall(Request.Builder().url(backend.url("/api/x")).build()).execute().close()

        assertEquals(emptyList<String>(), log)
    }

    @Test
    fun `an action is sent as described, with its JSON types`() = runBlocking {
        backend.enqueue(MockResponse().setBody("""{"is_favorite":true}"""))
        val body = buildJsonObject {
            put("movie_id", "550")
            put("favorite", true)
        }

        val answer = actions.send("post", "/api/favorite", body)

        assertEquals("""{"is_favorite":true}""", answer.getOrThrow())
        val request = backend.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/favorite", request.path)
        assertEquals("""{"movie_id":"550","favorite":true}""", request.body.readUtf8())
        assertTrue(request.getHeader("Content-Type")!!.startsWith("application/json"))
        assertEquals("Bearer test-session", request.getHeader("Authorization"))
    }

    @Test
    fun `an action cannot leave our backend or use an odd method`() = runBlocking {
        for ((method, endpoint) in listOf(
            "POST" to elsewhere.url("/api/favorite").toString(),
            "POST" to "//example.org/api/favorite",
            "TRACE" to "/api/favorite"
        )) {
            val error = actions.send(method, endpoint, null).exceptionOrNull()
            assertTrue("$method $endpoint", error is AppError.InvalidRequest)
        }
        assertEquals(0, backend.requestCount)
        assertEquals(0, elsewhere.requestCount)
    }

    @Test
    fun `an action failure is typed`() = runBlocking {
        backend.enqueue(
            MockResponse().setResponseCode(429).addHeader("Retry-After", "10")
                .setBody("""{"error":{"code":"RATE_LIMITED"}}""")
        )
        val limited = actions.send("POST", "/api/favorite", null).exceptionOrNull() as AppError.Http
        assertEquals("RATE_LIMITED", limited.serverCode)
        assertEquals(10, limited.retryAfterSeconds)

        backend.shutdown()
        val offline = actions.send("POST", "/api/favorite", null).exceptionOrNull()
        assertNotNull(offline)
        assertTrue(offline.toString(), offline is AppError.NoConnection)
    }
}
