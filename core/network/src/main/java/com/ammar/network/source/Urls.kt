package com.ammar.network.source

import okhttp3.HttpUrl
import okhttp3.OkHttpClient

internal fun HttpUrl.isSameOrigin(other: HttpUrl): Boolean =
    scheme == other.scheme && host == other.host && port == other.port

/** The sign-in endpoints, whose bodies carry the request token and the session id. */
internal fun HttpUrl.isAuthEndpoint(): Boolean =
    pathSegments.zipWithNext().any { (first, second) -> first == "api" && second == "auth" }

/**
 * For requests that carry data. A 307 or 308 redirect would send the body again, possibly to another
 * origin, and its answer would be taken for ours.
 */
internal fun OkHttpClient.withoutRedirects(): OkHttpClient =
    newBuilder().followRedirects(false).followSslRedirects(false).build()
