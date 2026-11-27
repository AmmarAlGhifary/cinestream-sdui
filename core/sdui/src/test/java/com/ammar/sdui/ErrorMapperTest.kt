package com.ammar.sdui

import com.ammar.cinestream.core.sdui.R
import com.ammar.network.error.AppError
import com.ammar.sdui.presentation.error.ErrorMapper
import com.ammar.sdui.presentation.error.isMovieNotFound
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.UnknownHostException

class ErrorMapperTest {

    private fun map(error: AppError) = ErrorMapper.map(error, includeTechnicalDetail = false)

    private data class Expected(val title: Int, val message: Int, val retryable: Boolean)

    private fun assertMapped(expected: Expected, error: AppError, label: String) {
        val content = map(error)
        assertEquals(label, expected, Expected(content.titleRes, content.messageRes, content.retryable))
    }

    @Test
    fun `each failure without a status has its own text`() {
        val cases = listOf(
            AppError.NoConnection() to
                Expected(R.string.error_no_connection_title, R.string.error_no_connection_message, true),
            AppError.Timeout() to Expected(R.string.error_timeout_title, R.string.error_timeout_message, true),
            AppError.SecureConnection() to
                Expected(R.string.error_secure_title, R.string.error_secure_message, true),
            AppError.Parse() to Expected(R.string.error_parse_title, R.string.error_parse_message, true),
            AppError.InvalidRequest() to
                Expected(R.string.error_invalid_request_title, R.string.error_invalid_request_message, false),
            AppError.Unknown() to Expected(R.string.error_unknown_title, R.string.error_unknown_message, true)
        )
        for ((error, expected) in cases) assertMapped(expected, error, error.javaClass.simpleName)
    }

    @Test
    fun `each status without a body has a fallback`() {
        val denied = Expected(R.string.error_http_denied_title, R.string.error_http_denied_message, false)
        val other4xx = Expected(R.string.error_http_4xx_title, R.string.error_http_4xx_message, false)
        val other5xx = Expected(R.string.error_http_5xx_title, R.string.error_http_5xx_message, true)
        val cases = mapOf(
            400 to Expected(R.string.error_http_400_title, R.string.error_http_400_message, false),
            401 to denied,
            403 to denied,
            404 to Expected(R.string.error_http_404_title, R.string.error_http_404_message, false),
            405 to other4xx,
            408 to Expected(R.string.error_http_408_title, R.string.error_http_408_message, true),
            418 to other4xx,
            429 to Expected(R.string.error_http_429_title, R.string.error_http_429_message, true),
            500 to Expected(R.string.error_http_500_title, R.string.error_http_500_message, true),
            501 to other5xx,
            502 to Expected(R.string.error_http_502_title, R.string.error_http_502_message, true),
            503 to Expected(R.string.error_http_503_title, R.string.error_http_503_message, true),
            504 to Expected(R.string.error_http_504_title, R.string.error_http_504_message, true),
            508 to other5xx,
            302 to Expected(R.string.error_unknown_title, R.string.error_unknown_message, true)
        )
        for ((status, expected) in cases) assertMapped(expected, AppError.Http(status), "HTTP $status")
    }

    private val contract = listOf(
        Triple("MISSING_PARAMETER", 400, Expected(R.string.error_code_missing_parameter_title, R.string.error_code_missing_parameter_message, false)),
        Triple("INVALID_PARAMETER", 400, Expected(R.string.error_code_invalid_parameter_title, R.string.error_code_invalid_parameter_message, false)),
        Triple("METHOD_NOT_ALLOWED", 405, Expected(R.string.error_code_method_not_allowed_title, R.string.error_code_method_not_allowed_message, false)),
        Triple("SCREEN_NOT_FOUND", 404, Expected(R.string.error_code_screen_not_found_title, R.string.error_code_screen_not_found_message, false)),
        Triple("MOVIE_NOT_FOUND", 404, Expected(R.string.error_code_movie_not_found_title, R.string.error_code_movie_not_found_message, false)),
        Triple("CONTENT_UNAVAILABLE", 503, Expected(R.string.error_code_content_unavailable_title, R.string.error_code_content_unavailable_message, true)),
        Triple("RATE_LIMITED", 429, Expected(R.string.error_code_rate_limited_title, R.string.error_code_rate_limited_message, true)),
        Triple("UPSTREAM_TIMEOUT", 504, Expected(R.string.error_code_upstream_timeout_title, R.string.error_code_upstream_timeout_message, true)),
        Triple("UPSTREAM_ERROR", 502, Expected(R.string.error_code_upstream_error_title, R.string.error_code_upstream_error_message, true)),
        Triple("SERVICE_MISCONFIGURED", 503, Expected(R.string.error_code_service_misconfigured_title, R.string.error_code_service_misconfigured_message, false)),
        Triple("MAINTENANCE", 503, Expected(R.string.error_code_maintenance_title, R.string.error_code_maintenance_message, true)),
        Triple("INTERNAL_ERROR", 500, Expected(R.string.error_code_internal_error_title, R.string.error_code_internal_error_message, true))
    )

    @Test
    fun `every contract code is mapped on the code, with or without the server's retry flag`() {
        for ((code, status, expected) in contract) {
            assertMapped(expected, AppError.Http(status, serverCode = code), code)
            assertMapped(expected, AppError.Http(status, serverCode = code, retryable = expected.retryable), code)
        }
    }

    @Test
    fun `the code wins over the status`() {
        val content = map(AppError.Http(500, serverCode = "MOVIE_NOT_FOUND"))

        assertEquals(R.string.error_code_movie_not_found_title, content.titleRes)
        assertFalse(content.retryable)
    }

    @Test
    fun `the server's retry flag wins for a known code`() {
        assertFalse(map(AppError.Http(500, serverCode = "INTERNAL_ERROR", retryable = false)).retryable)
        assertTrue(map(AppError.Http(404, serverCode = "MOVIE_NOT_FOUND", retryable = true)).retryable)
    }

    @Test
    fun `an unknown code is treated by its status and any 5xx can be retried`() {
        val on404 = map(AppError.Http(404, serverCode = "BRAND_NEW"))
        assertEquals(R.string.error_http_404_title, on404.titleRes)
        assertFalse(on404.retryable)

        val on503 = map(AppError.Http(503, serverCode = "BRAND_NEW", retryable = false))
        assertEquals(R.string.error_http_503_title, on503.titleRes)
        assertTrue(on503.retryable)

        assertTrue(map(AppError.Http(409, serverCode = "BRAND_NEW", retryable = true)).retryable)
    }

    @Test
    fun `server texts are preferred, each on its own`() {
        val both = map(AppError.Http(503, "MAINTENANCE", serverTitle = "Title", serverMessage = "Message"))
        assertEquals("Title", both.serverTitle)
        assertEquals("Message", both.serverMessage)

        val onlyMessage = map(AppError.Http(503, serverMessage = "Message"))
        assertNull(onlyMessage.serverTitle)
        assertEquals("Message", onlyMessage.serverMessage)
    }

    @Test
    fun `server text that looks technical is not shown`() {
        for (text in listOf(
            "Failed to fetch https://example.org/movie",
            "java.lang.NullPointerException at Foo",
            "x".repeat(301)
        )) {
            val content = map(AppError.Http(500, serverTitle = text, serverMessage = text))
            assertNull(content.serverTitle)
            assertNull(content.serverMessage)
        }
    }

    @Test
    fun `retry after is only kept when retrying is possible`() {
        assertEquals(30, map(AppError.Http(503, "CONTENT_UNAVAILABLE", retryAfterSeconds = 30)).retryAfterSeconds)
        assertNull(map(AppError.Http(503, "SERVICE_MISCONFIGURED", retryAfterSeconds = 30)).retryAfterSeconds)
    }

    @Test
    fun `technical detail is left out unless asked for`() {
        val http = AppError.Http(503, serverCode = "MAINTENANCE", requestId = "req-1")
        val offline = AppError.NoConnection(UnknownHostException("secret.host"))

        assertNull(map(http).technicalDetail)
        assertNull(map(offline).technicalDetail)
        assertEquals(
            "HTTP 503 · MAINTENANCE · request req-1",
            ErrorMapper.map(http, includeTechnicalDetail = true).technicalDetail
        )
        assertEquals(
            "NoConnection · UnknownHostException",
            ErrorMapper.map(offline, includeTechnicalDetail = true).technicalDetail
        )
    }

    @Test
    fun `only a missing movie counts as movie not found`() {
        assertTrue(AppError.Http(404, serverCode = "MOVIE_NOT_FOUND").isMovieNotFound())
        assertFalse(AppError.Http(404).isMovieNotFound())
        assertFalse(AppError.NoConnection().isMovieNotFound())
    }
}
