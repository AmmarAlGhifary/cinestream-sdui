package com.ammar.network.source

import okhttp3.HttpUrl
import okhttp3.OkHttpClient

internal fun HttpUrl.isSameOrigin(other: HttpUrl): Boolean = origin == other.origin

internal val HttpUrl.origin: String get() = "$scheme://$host:$port"

/** The sign-in endpoints, whose bodies carry the request token, the secret and the session id. */
internal fun HttpUrl.isAuthEndpoint(): Boolean =
    segments().zipWithNext().any { (first, second) ->
        first.equals("api", ignoreCase = true) && second.equals("auth", ignoreCase = true)
    }

/** Where a server-driven action may go: something under /api/ that is not a sign-in endpoint. */
internal fun HttpUrl.isActionEndpoint(): Boolean {
    val segments = segments()
    // An encoded slash could be read as a path separator by the server, hiding where it leads.
    return segments.size > 1 && segments.first() == "api" &&
        segments.none { '/' in it || '\\' in it } && !isAuthEndpoint()
}

/** The path without empty segments, so that "api//auth" reads like "api/auth". */
private fun HttpUrl.segments(): List<String> = pathSegments.filter { it.isNotEmpty() }

/**
 * For requests that carry data. A 307 or 308 redirect would send the body again, possibly to another
 * origin, and its answer would be taken for ours.
 */
internal fun OkHttpClient.withoutRedirects(): OkHttpClient =
    newBuilder().followRedirects(false).followSslRedirects(false).build()
