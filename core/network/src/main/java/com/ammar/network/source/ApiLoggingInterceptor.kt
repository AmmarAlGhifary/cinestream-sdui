package com.ammar.network.source

import okhttp3.Interceptor
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor

/**
 * HTTP logging for debug builds. The session header is always redacted, and the sign-in endpoints
 * are logged without bodies because those hold the request token and the session id.
 */
internal class ApiLoggingInterceptor(
    enabled: Boolean,
    logger: HttpLoggingInterceptor.Logger = HttpLoggingInterceptor.Logger.DEFAULT
) : Interceptor {

    private val withBodies = logging(logger, if (enabled) Level.BODY else Level.NONE)
    private val withoutBodies = logging(logger, if (enabled) Level.HEADERS else Level.NONE)

    override fun intercept(chain: Interceptor.Chain): Response {
        val delegate = if (chain.request().url.isAuthEndpoint()) withoutBodies else withBodies
        return delegate.intercept(chain)
    }

    private fun logging(logger: HttpLoggingInterceptor.Logger, wanted: Level) =
        HttpLoggingInterceptor(logger).apply {
            level = wanted
            redactHeader("Authorization")
        }
}

private typealias Level = HttpLoggingInterceptor.Level
