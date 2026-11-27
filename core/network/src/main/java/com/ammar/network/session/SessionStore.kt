package com.ammar.network.session

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Clock

// Plain classes on purpose: a data class would print the session id or token from toString().
@Serializable
internal class StoredSession(
    val sessionId: String,
    val accountId: String,
    val username: String,
    /** The backend that issued it. It is never sent to another one. */
    val origin: String
)

/** The sign-in in progress. Stored, because the app may be killed while the user is in the browser. */
@Serializable
internal class PendingSignIn(
    val requestToken: String,
    /** Proves to the backend that whoever creates the session is who asked for the token. */
    val secret: String,
    val expiresAtMillis: Long,
    val origin: String
)

/**
 * The stored session and pending sign-in, encrypted at rest. Safe to use from any thread: the UI
 * reads [state] while OkHttp threads read the id and may clear it.
 *
 * Storage and Keystore are slow, so everything except [state] must be called off the main thread,
 * which [onIo] does. Loading starts on [io] when this is created; until it has finished, [state]
 * reads as signed out.
 *
 * @param origin the backend in use. Anything stored for another one is discarded.
 */
class SessionStore internal constructor(
    private val storage: EncryptedStorage,
    private val origin: String,
    private val clock: Clock,
    private val io: CoroutineDispatcher
) {

    private val lock = Any()
    private var loaded = false
    private var session: StoredSession? = null
    private val _state = MutableStateFlow<SessionState>(SessionState.SignedOut)

    /** The token of a sign-in that ran out, so a late approval for it can still be explained. */
    private var lapsedToken: String? = null

    val state: StateFlow<SessionState> = _state.asStateFlow()

    init {
        CoroutineScope(io).launch { locked { } }
    }

    internal suspend fun <T> onIo(block: SessionStore.() -> T): T = withContext(io) { block() }

    internal fun sessionId(): String? = locked { session?.sessionId }

    internal fun save(sessionId: String, account: Account) = locked {
        val saved = StoredSession(sessionId, account.id, account.username, origin)
        write(SESSION, Json.encodeToString(StoredSession.serializer(), saved))
        show(saved)
    }

    internal fun clearSession() = locked {
        storage.remove(SESSION)
        show(null)
    }

    /** A late answer about an old session must not sign out a newer one. */
    internal fun clearSessionIfCurrent(sessionId: String) = locked {
        if (session?.sessionId == sessionId) clearSession()
    }

    internal fun pending(): PendingSignIn? = locked { readPending() }

    internal fun savePending(requestToken: String, secret: String, expiresAtMillis: Long) = locked {
        val pending = PendingSignIn(requestToken, secret, expiresAtMillis, origin)
        write(PENDING, Json.encodeToString(PendingSignIn.serializer(), pending))
    }

    internal fun clearPending() = locked {
        storage.remove(PENDING)
        lapsedToken = null
    }

    /** Leaves a newer sign-in alone when an older one finishes. */
    internal fun clearPendingIfToken(requestToken: String) = locked {
        if (readPending()?.requestToken == requestToken) clearPending()
    }

    /** Removes a pending sign-in, secret included, once it can no longer be completed. */
    internal fun dropExpiredPending() = locked { dropIfExpired() }

    /** True once, for the token of a sign-in that expired. */
    internal fun takeLapsed(requestToken: String): Boolean = locked {
        val matches = lapsedToken == requestToken
        if (matches) lapsedToken = null
        matches
    }

    private inline fun <T> locked(block: () -> T): T = synchronized(lock) {
        load()
        block()
    }

    private fun load() {
        if (loaded) return
        loaded = true
        // Storage that cannot be read means signed out; it must not take the app down.
        runCatching {
            show(read(SESSION, StoredSession.serializer()) { it.origin })
            dropIfExpired()
        }
    }

    private fun readPending(): PendingSignIn? = read(PENDING, PendingSignIn.serializer()) { it.origin }

    private fun dropIfExpired() {
        val pending = readPending() ?: return
        if (clock.millis() >= pending.expiresAtMillis) {
            storage.remove(PENDING)
            lapsedToken = pending.requestToken
        }
    }

    /** Null, and removed, if it cannot be understood or belongs to another backend. */
    private fun <T : Any> read(name: String, serializer: KSerializer<T>, originOf: (T) -> String): T? {
        val text = storage.read(name) ?: return null
        val value = runCatching { Json.decodeFromString(serializer, text) }.getOrNull()
        if (value == null || originOf(value) != origin) {
            storage.remove(name)
            return null
        }
        return value
    }

    private fun write(name: String, value: String) {
        // A new key means the stored session went with the old one.
        if (storage.write(name, value)) show(null)
    }

    private fun show(newSession: StoredSession?) {
        session = newSession
        _state.value =
            if (newSession == null) SessionState.SignedOut
            else SessionState.SignedIn(Account(newSession.accountId, newSession.username))
    }

    private companion object {
        const val SESSION = "session"
        const val PENDING = "pending_sign_in"
    }
}
