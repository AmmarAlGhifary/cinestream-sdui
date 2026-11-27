package com.ammar.network.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Clock

// Plain classes on purpose: a data class would print the session id or token from toString().
@Serializable
internal class StoredSession(val sessionId: String, val accountId: String, val username: String)

/** The sign-in in progress. Stored, because the app may be killed while the user is in the browser. */
@Serializable
internal class PendingSignIn(
    val requestToken: String,
    /** Proves to the backend that whoever creates the session is who asked for the token. */
    val secret: String,
    val expiresAtMillis: Long
)

/**
 * The stored session and pending sign-in, encrypted at rest. Safe to use from any thread: the UI
 * reads [state] while OkHttp threads read the id and may clear it.
 */
class SessionStore internal constructor(
    private val storage: EncryptedStorage,
    private val clock: Clock
) {

    private val lock = Any()
    private var session: StoredSession? = read(SESSION)
    private val _state = MutableStateFlow(stateOf(session))

    /** The token of a sign-in that ran out, so a late approval for it can still be explained. */
    private var lapsedToken: String? = null

    init {
        dropExpiredPending()
    }

    val state: StateFlow<SessionState> = _state.asStateFlow()

    internal fun sessionId(): String? = synchronized(lock) { session?.sessionId }

    internal fun save(newSession: StoredSession) = synchronized(lock) {
        write(SESSION, Json.encodeToString(StoredSession.serializer(), newSession))
        session = newSession
        _state.value = stateOf(newSession)
    }

    internal fun clearSession() = synchronized(lock) {
        storage.remove(SESSION)
        session = null
        _state.value = SessionState.SignedOut
    }

    /** A late answer about an old session must not sign out a newer one. */
    internal fun clearSessionIfCurrent(sessionId: String) = synchronized(lock) {
        if (session?.sessionId == sessionId) clearSession()
    }

    internal fun pending(): PendingSignIn? = synchronized(lock) { read(PENDING) }

    internal fun savePending(pending: PendingSignIn) = synchronized(lock) {
        write(PENDING, Json.encodeToString(PendingSignIn.serializer(), pending))
    }

    internal fun clearPending() = synchronized(lock) {
        storage.remove(PENDING)
        lapsedToken = null
    }

    /** Removes a pending sign-in, secret included, once it can no longer be completed. */
    internal fun dropExpiredPending() = synchronized(lock) {
        val pending = pending()
        if (pending != null && clock.millis() >= pending.expiresAtMillis) {
            storage.remove(PENDING)
            lapsedToken = pending.requestToken
        }
    }

    /** True once, for the token of a sign-in that expired. */
    internal fun takeLapsed(requestToken: String): Boolean = synchronized(lock) {
        val matches = lapsedToken == requestToken
        if (matches) lapsedToken = null
        matches
    }

    private fun write(name: String, value: String) {
        // A new key means the stored session went with the old one.
        if (storage.write(name, value)) {
            session = null
            _state.value = SessionState.SignedOut
        }
    }

    private inline fun <reified T> read(name: String): T? =
        storage.read(name)?.let { runCatching { Json.decodeFromString<T>(it) }.getOrNull() }

    private fun stateOf(session: StoredSession?): SessionState =
        if (session == null) SessionState.SignedOut
        else SessionState.SignedIn(Account(session.accountId, session.username))

    private companion object {
        const val SESSION = "session"
        const val PENDING = "pending_sign_in"
    }
}
