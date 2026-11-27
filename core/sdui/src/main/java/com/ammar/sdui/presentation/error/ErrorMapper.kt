package com.ammar.sdui.presentation.error

import androidx.annotation.StringRes
import com.ammar.cinestream.core.sdui.R
import com.ammar.network.error.AppError

/**
 * What the error dialog shows. The server texts win over the resources when present.
 * [technicalDetail] is only filled when the caller asks for it, which release builds never do.
 */
data class ErrorContent(
    @param:StringRes val titleRes: Int,
    @param:StringRes val messageRes: Int,
    val retryable: Boolean,
    val serverTitle: String? = null,
    val serverMessage: String? = null,
    val retryAfterSeconds: Int? = null,
    val technicalDetail: String? = null
)

/** The movie this screen is about does not exist, so staying on the screen is pointless. */
fun AppError.isMovieNotFound(): Boolean = this is AppError.Http && serverCode == ErrorMapper.MOVIE_NOT_FOUND

/**
 * The single place that turns an [AppError] into user-facing text and a retry decision.
 *
 * HTTP errors are matched on the backend's error code first and on the status second.
 */
object ErrorMapper {

    internal const val MOVIE_NOT_FOUND = "MOVIE_NOT_FOUND"

    private const val MAX_SERVER_TEXT = 300

    private data class Fallback(
        @param:StringRes val titleRes: Int,
        @param:StringRes val messageRes: Int,
        val retryable: Boolean
    )

    private val byCode: Map<String, Fallback> = mapOf(
        "MISSING_PARAMETER" to Fallback(
            R.string.error_code_missing_parameter_title,
            R.string.error_code_missing_parameter_message,
            retryable = false
        ),
        "INVALID_PARAMETER" to Fallback(
            R.string.error_code_invalid_parameter_title,
            R.string.error_code_invalid_parameter_message,
            retryable = false
        ),
        "METHOD_NOT_ALLOWED" to Fallback(
            R.string.error_code_method_not_allowed_title,
            R.string.error_code_method_not_allowed_message,
            retryable = false
        ),
        "SCREEN_NOT_FOUND" to Fallback(
            R.string.error_code_screen_not_found_title,
            R.string.error_code_screen_not_found_message,
            retryable = false
        ),
        MOVIE_NOT_FOUND to Fallback(
            R.string.error_code_movie_not_found_title,
            R.string.error_code_movie_not_found_message,
            retryable = false
        ),
        "CONTENT_UNAVAILABLE" to Fallback(
            R.string.error_code_content_unavailable_title,
            R.string.error_code_content_unavailable_message,
            retryable = true
        ),
        "RATE_LIMITED" to Fallback(
            R.string.error_code_rate_limited_title,
            R.string.error_code_rate_limited_message,
            retryable = true
        ),
        "UPSTREAM_TIMEOUT" to Fallback(
            R.string.error_code_upstream_timeout_title,
            R.string.error_code_upstream_timeout_message,
            retryable = true
        ),
        "UPSTREAM_ERROR" to Fallback(
            R.string.error_code_upstream_error_title,
            R.string.error_code_upstream_error_message,
            retryable = true
        ),
        "SERVICE_MISCONFIGURED" to Fallback(
            R.string.error_code_service_misconfigured_title,
            R.string.error_code_service_misconfigured_message,
            retryable = false
        ),
        "MAINTENANCE" to Fallback(
            R.string.error_code_maintenance_title,
            R.string.error_code_maintenance_message,
            retryable = true
        ),
        "INTERNAL_ERROR" to Fallback(
            R.string.error_code_internal_error_title,
            R.string.error_code_internal_error_message,
            retryable = true
        )
    )

    private val unknown = Fallback(R.string.error_unknown_title, R.string.error_unknown_message, retryable = true)

    fun map(error: AppError, includeTechnicalDetail: Boolean): ErrorContent {
        val content = when (error) {
            is AppError.Http -> mapHttp(error)
            is AppError.NoConnection -> Fallback(
                R.string.error_no_connection_title, R.string.error_no_connection_message, retryable = true
            ).toContent()
            is AppError.Timeout -> Fallback(
                R.string.error_timeout_title, R.string.error_timeout_message, retryable = true
            ).toContent()
            is AppError.SecureConnection -> Fallback(
                R.string.error_secure_title, R.string.error_secure_message, retryable = true
            ).toContent()
            is AppError.Parse -> Fallback(
                R.string.error_parse_title, R.string.error_parse_message, retryable = true
            ).toContent()
            is AppError.InvalidRequest -> Fallback(
                R.string.error_invalid_request_title, R.string.error_invalid_request_message, retryable = false
            ).toContent()
            is AppError.Unknown -> unknown.toContent()
        }
        return if (includeTechnicalDetail) content.copy(technicalDetail = technicalDetail(error)) else content
    }

    private fun mapHttp(error: AppError.Http): ErrorContent {
        val known = error.serverCode?.let { byCode[it] }
        val fallback = known ?: byStatus(error.status)
        val retryable = when {
            known != null -> error.retryable ?: known.retryable
            // Without a code we recognise, a server-side failure is always worth another try.
            error.status in 500..599 -> true
            else -> error.retryable ?: fallback.retryable
        }
        return fallback.toContent().copy(
            retryable = retryable,
            serverTitle = error.serverTitle.displayable(),
            serverMessage = error.serverMessage.displayable(),
            retryAfterSeconds = error.retryAfterSeconds.takeIf { retryable }
        )
    }

    private fun byStatus(status: Int): Fallback = when (status) {
        400 -> Fallback(R.string.error_http_400_title, R.string.error_http_400_message, retryable = false)
        401, 403 -> Fallback(R.string.error_http_denied_title, R.string.error_http_denied_message, retryable = false)
        404 -> Fallback(R.string.error_http_404_title, R.string.error_http_404_message, retryable = false)
        408 -> Fallback(R.string.error_http_408_title, R.string.error_http_408_message, retryable = true)
        429 -> Fallback(R.string.error_http_429_title, R.string.error_http_429_message, retryable = true)
        in 400..499 -> Fallback(R.string.error_http_4xx_title, R.string.error_http_4xx_message, retryable = false)
        500 -> Fallback(R.string.error_http_500_title, R.string.error_http_500_message, retryable = true)
        502 -> Fallback(R.string.error_http_502_title, R.string.error_http_502_message, retryable = true)
        503 -> Fallback(R.string.error_http_503_title, R.string.error_http_503_message, retryable = true)
        504 -> Fallback(R.string.error_http_504_title, R.string.error_http_504_message, retryable = true)
        in 500..599 -> Fallback(R.string.error_http_5xx_title, R.string.error_http_5xx_message, retryable = true)
        else -> unknown
    }

    private fun Fallback.toContent() = ErrorContent(titleRes, messageRes, retryable)

    /** Server text is shown to users, so anything that looks like a URL or an exception is dropped. */
    private fun String?.displayable(): String? = this?.takeIf {
        it.isNotBlank() && it.length <= MAX_SERVER_TEXT && "://" !in it && "Exception" !in it
    }

    /** For debug builds: status, code and exception class. Never a message, host or URL. */
    private fun technicalDetail(error: AppError): String = when (error) {
        is AppError.Http -> listOfNotNull(
            "HTTP ${error.status}",
            error.serverCode,
            error.requestId?.let { "request $it" }
        )
        else -> listOfNotNull(error.javaClass.simpleName, error.cause?.javaClass?.simpleName)
    }.joinToString(" · ")
}
