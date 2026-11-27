package com.ammar.network.source

import okhttp3.HttpUrl

internal fun HttpUrl.isSameOrigin(other: HttpUrl): Boolean =
    scheme == other.scheme && host == other.host && port == other.port

/** The sign-in endpoints, whose bodies carry the request token and the session id. */
internal fun HttpUrl.isAuthEndpoint(): Boolean =
    pathSegments.zipWithNext().any { (first, second) -> first == "api" && second == "auth" }
