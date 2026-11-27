package com.ammar.network.error

import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

/**
 * Every way loading a screen can fail. This is what the data layers report; the text a user reads is
 * chosen from it in one place (ErrorMapper). Never show an exception message or a cause to users.
 */
sealed class AppError(cause: Throwable? = null) : Exception(cause) {

    class NoConnection(cause: Throwable? = null) : AppError(cause)

    class Timeout(cause: Throwable? = null) : AppError(cause)

    class SecureConnection(cause: Throwable? = null) : AppError(cause)

    /** A non-2xx answer. Everything except [status] is optional and comes from the server. */
    class Http(
        val status: Int,
        val serverCode: String? = null,
        val serverTitle: String? = null,
        val serverMessage: String? = null,
        val retryable: Boolean? = null,
        val requestId: String? = null,
        val retryAfterSeconds: Int? = null
    ) : AppError()

    /** The server answered, but not with a screen this app can read. */
    class Parse(cause: Throwable? = null) : AppError(cause)

    /** The sign-in link was not the approval this app asked for, or it came too late. */
    class SignInFailed : AppError()

    /** The app was asked to open a screen without the details that screen needs. */
    class InvalidRequest : AppError()

    class Unknown(cause: Throwable? = null) : AppError(cause)
}

fun Throwable.toAppError(): AppError = when (this) {
    is AppError -> this
    is UnknownHostException, is ConnectException, is NoRouteToHostException -> AppError.NoConnection(this)
    is SocketTimeoutException -> AppError.Timeout(this)
    is SSLException -> AppError.SecureConnection(this)
    else -> AppError.Unknown(this)
}
