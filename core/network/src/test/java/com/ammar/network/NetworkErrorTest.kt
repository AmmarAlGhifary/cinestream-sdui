package com.ammar.network

import com.ammar.network.error.AppError
import com.ammar.network.error.toAppError
import com.ammar.network.source.NetworkDataSourceImpl
import com.ammar.network.source.SduiApiService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.BufferedSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import kotlin.coroutines.cancellation.CancellationException

class NetworkErrorTest {

    private fun source(answer: () -> Response<ResponseBody>) = NetworkDataSourceImpl(
        object : SduiApiService {
            override suspend fun getScreenBlueprint(screenId: String, queryParams: Map<String, String>) = answer()
        }
    )

    private fun fetch(answer: () -> Response<ResponseBody>): Result<String> =
        runBlocking { source(answer).getScreenBlueprint("home").first() }

    private fun failure(
        status: Int,
        body: String,
        contentType: String = "application/json",
        retryAfter: String? = null
    ): AppError.Http {
        val raw = okhttp3.Response.Builder()
            .code(status)
            .message("status message")
            .protocol(Protocol.HTTP_1_1)
            .request(Request.Builder().url("http://localhost/").build())
        if (retryAfter != null) raw.header("Retry-After", retryAfter)
        val result = fetch { Response.error(body.toResponseBody(contentType.toMediaType()), raw.build()) }
        return result.exceptionOrNull() as AppError.Http
    }

    private fun assertStatusOnly(error: AppError.Http, status: Int) {
        assertEquals(status, error.status)
        assertNull(error.serverCode)
        assertNull(error.serverTitle)
        assertNull(error.serverMessage)
        assertNull(error.retryable)
        assertNull(error.requestId)
    }

    @Test
    fun `each connection exception maps to its error type`() {
        val cases = listOf(
            UnknownHostException("secret.host") to AppError.NoConnection::class,
            ConnectException() to AppError.NoConnection::class,
            NoRouteToHostException() to AppError.NoConnection::class,
            SocketTimeoutException() to AppError.Timeout::class,
            SSLHandshakeException("bad certificate") to AppError.SecureConnection::class,
            SSLPeerUnverifiedException("wrong host") to AppError.SecureConnection::class,
            IOException("stream reset") to AppError.Unknown::class,
            IllegalStateException() to AppError.Unknown::class
        )
        for ((thrown, expected) in cases) {
            val error = fetch { throw thrown }.exceptionOrNull()!!
            assertEquals(thrown.javaClass.simpleName, expected, error::class)
            assertSame(thrown, error.cause)
        }
    }

    @Test
    fun `an app error is not wrapped twice`() {
        val error = AppError.Parse()
        assertSame(error, error.toAppError())
    }

    @Test
    fun `success passes the body through and emits once`() = runBlocking {
        val results = source { Response.success("{}".toResponseBody()) }.getScreenBlueprint("home").toList()

        assertEquals(listOf("{}"), results.map { it.getOrThrow() })
    }

    @Test
    fun `a success without a body is a parse error`() {
        assertTrue(fetch { Response.success<ResponseBody>(null) }.exceptionOrNull() is AppError.Parse)
    }

    @Test
    fun `cancellation is passed on instead of being reported as an error`() {
        assertThrows(CancellationException::class.java) { fetch { throw CancellationException("cancelled") } }
    }

    @Test
    fun `the full error body is read`() {
        val error = failure(
            429,
            """{"error":{"code":"RATE_LIMITED","title":"Slow down","message":"Wait a bit.",
                "retryable":true,"request_id":"fra1::abc-123"}}""",
            retryAfter = "10"
        )

        assertEquals(429, error.status)
        assertEquals("RATE_LIMITED", error.serverCode)
        assertEquals("Slow down", error.serverTitle)
        assertEquals("Wait a bit.", error.serverMessage)
        assertEquals(true, error.retryable)
        assertEquals("fra1::abc-123", error.requestId)
        assertEquals(10, error.retryAfterSeconds)
    }

    @Test
    fun `every field of the error body is optional`() {
        assertStatusOnly(failure(500, """{"error":{}}"""), 500)

        val error = failure(404, """{"error":{"code":"MOVIE_NOT_FOUND"}}""")
        assertEquals("MOVIE_NOT_FOUND", error.serverCode)
        assertNull(error.serverTitle)
        assertNull(error.retryable)
    }

    @Test
    fun `fields of the wrong type or blank are ignored one by one`() {
        val error = failure(
            503,
            """{"error":{"code":7,"title":"  ","message":["x"],"retryable":"false","request_id":{"a":1}}}"""
        )

        assertStatusOnly(error, 503)
    }

    @Test
    fun `a code or request id that is not a plain token is dropped`() {
        val error = failure(500, """{"error":{"code":"see http://host/x","request_id":"a b\nc","title":"T"}}""")

        assertNull(error.serverCode)
        assertNull(error.requestId)
        assertEquals("T", error.serverTitle)
    }

    @Test
    fun `a missing or malformed body leaves only the status`() {
        val bodies = listOf(
            "",
            "{",
            "null",
            "[]",
            """{"message":"no error object"}""",
            """{"error":null}""",
            """{"error":[1]}""",
            "{\"error\":" + "[".repeat(30_000) + "]".repeat(30_000) + "}",
            """{"error":{"title":"${"x".repeat(70_000)}"}}"""
        )
        for (body in bodies) assertStatusOnly(failure(502, body), 502)
    }

    @Test
    fun `an oversized body of unknown length is ignored`() {
        val text = """{"error":{"title":"${"x".repeat(70_000)}"}}"""
        val unknownLength = object : ResponseBody() {
            private val buffer = Buffer().writeUtf8(text)
            override fun contentType() = "application/json".toMediaType()
            override fun contentLength() = -1L
            override fun source(): BufferedSource = buffer
        }

        val result = fetch { Response.error(502, unknownLength) }

        assertStatusOnly(result.exceptionOrNull() as AppError.Http, 502)
    }

    @Test
    fun `the legacy string body is accepted and its text is not kept`() {
        val error = failure(500, """{"error":"TypeError: cannot read properties of undefined"}""")

        assertStatusOnly(error, 500)
    }

    @Test
    fun `a text body from the platform is accepted`() {
        for (status in listOf(404, 502, 504, 508)) {
            val body = "The deployment failed.\nDEPLOYMENT_NOT_FOUND"
            assertStatusOnly(failure(status, body, contentType = "text/plain"), status)
        }
    }

    @Test
    fun `retry after is read as a number of seconds`() {
        fun seconds(header: String?) = failure(503, "", retryAfter = header).retryAfterSeconds

        assertEquals(30, seconds("30"))
        assertEquals(300, seconds(" 300 "))
        assertEquals(86_400, seconds("86400"))
        for (ignored in listOf(null, "", "0", "-5", "1.5", "soon", "86401", "99999999999",
            "Wed, 21 Oct 2026 07:28:00 GMT")) {
            assertNull("Retry-After: $ignored", seconds(ignored))
        }
    }
}
