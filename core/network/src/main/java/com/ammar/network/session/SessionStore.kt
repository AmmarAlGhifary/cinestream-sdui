package com.ammar.network.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

// Plain classes on purpose: a data class would print the session id or token from toString().
@Serializable
internal class StoredSession(val sessionId: String, val accountId: String, val username: String)

/** The sign-in in progress. Stored, because the app may be killed while the user is in the browser. */
@Serializable
internal class PendingSignIn(val requestToken: String, val expiresAtMillis: Long)

/**
 * The stored session and pending sign-in, encrypted at rest. Safe to use from any thread: the UI
 * reads [state] while OkHttp threads read the id and may clear it.
 */
class SessionStore internal constructor(private val storage: EncryptedStorage) {

    private val lock = Any()
    private var session: StoredSession? = read(SESSION)
    private val _state = MutableStateFlow(stateOf(session))

    val state: StateFlow<SessionState> = _state.asStateFlow()

    internal fun sessionId(): String? = synchronized(lock) { session?.sessionId }

    internal fun save(newSession: StoredSession) = synchronized(lock) {
        storage.write(SESSION, Json.encodeToString(StoredSession.serializer(), newSession))
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
        storage.write(PENDING, Json.encodeToString(PendingSignIn.serializer(), pending))
    }

    internal fun clearPending() = synchronized(lock) { storage.remove(PENDING) }

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
