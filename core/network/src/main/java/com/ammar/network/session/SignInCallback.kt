package com.ammar.network.session

import java.net.URI
import java.net.URLDecoder
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/** The deep link TMDB redirects to: `cinestream://auth/tmdb?request_token=...&approved=true`. */
class SignInCallback internal constructor(
    internal val requestToken: String?,
    internal val approved: Boolean
) {
    companion object {
        /** Null for any other link. Any app can send this link, so nothing in it is trusted yet. */
        fun parse(link: String?): SignInCallback? {
            val uri = link?.let { runCatching { URI(it) }.getOrNull() } ?: return null
            if (uri.scheme != "cinestream" || uri.rawAuthority != "auth" || uri.path != "/tmdb") return null

            val parameters = uri.rawQuery.orEmpty().split('&').map { pair ->
                pair.substringBefore('=') to decode(pair.substringAfter('=', ""))
            }
            // A repeated parameter is ambiguous, so it counts as absent.
            fun single(name: String) = parameters.filter { it.first == name }.singleOrNull()?.second
            return SignInCallback(
                requestToken = single("request_token")?.takeIf { it.isNotEmpty() },
                approved = single("approved") == "true"
            )
        }

        private fun decode(value: String): String? =
            runCatching { URLDecoder.decode(value, "UTF-8") }.getOrNull()
    }
}

private val TMDB_DATE = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")

/** The contract does not fix a format for `expires_at`: ISO 8601 and TMDB's own form are read. */
internal fun parseExpiry(value: String?): Instant? {
    val text = value?.trim() ?: return null
    return runCatching { Instant.parse(text) }
        .recoverCatching { OffsetDateTime.parse(text).toInstant() }
        .recoverCatching { LocalDateTime.parse(text, TMDB_DATE).toInstant(ZoneOffset.UTC) }
        .getOrNull()
}
