package com.ammar.network.source

import com.ammar.network.error.HttpErrorParser
import com.ammar.network.session.SessionStore
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Signs requests to our own backend with the stored session, and forgets that session when the
 * backend says it is dead. Other hosts never see it; OkHttp also drops the header on a redirect to
 * another origin.
 */
internal class AuthInterceptor(
    private val sessions: SessionStore,
    private val baseUrl: HttpUrl
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val sessionId = sessions.sessionId()
        // Getting a token and creating a session are the calls that happen without one.
        val signingIn = request.method == "POST" && request.url.isAuthEndpoint()
        if (sessionId == null || signingIn || !request.url.isSameOrigin(baseUrl)) return chain.proceed(request)

        val response = chain.proceed(
            request.newBuilder().header("Authorization", "Bearer $sessionId").build()
        )
        if (response.request.url.isSameOrigin(baseUrl) && response.saysSessionIsDead()) {
            sessions.clearSessionIfCurrent(sessionId)
        }
        return response
    }

    private fun Response.saysSessionIsDead(): Boolean {
        if (header("X-Session-Invalid").equals("true", ignoreCase = true)) return true
        if (isSuccessful) return false
        // A copy is read, so the caller still gets the whole body.
        val copy = runCatching { peekBody(PEEK_BYTES) }.getOrNull()
        return HttpErrorParser.parse(code, headers, copy).serverCode == "SESSION_EXPIRED"
    }

    private companion object {
        const val PEEK_BYTES = 64 * 1024L + 1
    }
}
