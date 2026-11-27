package com.ammar.network.session

import com.ammar.network.error.AppError
import com.ammar.network.source.AuthApiService
import com.ammar.network.source.apiResult
import com.ammar.network.source.bodyOrError
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.time.Clock
import java.time.Duration

internal class SessionRepositoryImpl(
    private val api: AuthApiService,
    private val store: SessionStore,
    private val clock: Clock
) : SessionRepository {

    private val completing = Mutex()
    private val _signIns = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override val session: StateFlow<SessionState> get() = store.state

    override val signIns: Flow<Unit> = _signIns.asSharedFlow()

    override suspend fun startSignIn(): Result<String> = apiResult {
        // A new sign-in replaces the old one, even if this request then fails.
        store.onIo { clearPending() }
        val answer = parse<RequestTokenAnswer>(api.requestToken().bodyOrError().getOrThrow())
        // The page gets the user's TMDB password, so nothing but TMDB over HTTPS is opened.
        val page = answer.approveUrl.toHttpUrlOrNull()?.takeIf { it.isHttps && it.host == APPROVAL_HOST }
        if (page == null || answer.requestToken.isBlank() || answer.signInSecret.isBlank()) {
            throw AppError.Parse()
        }
        val expiresAt = parseExpiry(answer.expiresAt) ?: clock.instant().plus(DEFAULT_LIFETIME)
        store.onIo { savePending(answer.requestToken, answer.signInSecret, expiresAt.toEpochMilli()) }
        // The checked address is passed on, so the browser cannot read it differently.
        Result.success(page.toString())
    }

    override suspend fun completeSignIn(link: String): Result<SignInOutcome> = completing.withLock {
        when (val check = store.onIo { check(SignInCallback.parse(link)) }) {
            LinkCheck.Stray -> Result.success(SignInOutcome.Ignored)
            LinkCheck.Refused -> Result.failure(AppError.SignInFailed())
            is LinkCheck.Approved -> createSession(check.pending)
        }
    }

    private sealed interface LinkCheck {
        /** Not about the sign-in being waited for. */
        data object Stray : LinkCheck

        /** Declined, or too late. */
        data object Refused : LinkCheck
        class Approved(val pending: PendingSignIn) : LinkCheck
    }

    private fun SessionStore.check(callback: SignInCallback?): LinkCheck {
        val token = callback?.requestToken ?: return LinkCheck.Stray
        dropExpiredPending()
        if (takeLapsed(token)) return LinkCheck.Refused
        // A link that does not carry our token must neither cancel the real sign-in nor bother anyone.
        val pending = pending()?.takeIf { it.requestToken == token } ?: return LinkCheck.Stray
        if (!callback.approved) {
            clearPendingIfToken(token)
            return LinkCheck.Refused
        }
        return LinkCheck.Approved(pending)
    }

    private suspend fun createSession(pending: PendingSignIn): Result<SignInOutcome> {
        val result = apiResult<SignInOutcome> {
            val body = buildJsonObject {
                put("request_token", pending.requestToken)
                put("sign_in_secret", pending.secret)
            }.toString()
            val answer = api.createSession(body.toRequestBody(JSON)).bodyOrError().getOrThrow()
            val created = parse<SessionAnswer>(answer)
            // It becomes a header value, so anything but visible ASCII is rejected here.
            if (!HEADER_SAFE.matches(created.sessionId)) throw AppError.Parse()
            val account = Account(created.account.id.content, created.account.username)
            store.onIo {
                save(created.sessionId, account)
                clearPendingIfToken(pending.requestToken)
            }
            _signIns.tryEmit(Unit)
            Result.success(SignInOutcome.SignedIn(account))
        }
        // A refused token cannot be used again; after a network or server failure it still can.
        val refusal = (result.exceptionOrNull() as? AppError.Http)
            ?.takeIf { it.status in 400..499 && it.status != 408 && it.status != 429 }
            ?: return result
        store.onIo { clearPendingIfToken(pending.requestToken) }
        // To the user a refused sign-in is a failed one, whatever the backend objected to.
        return if (refusal.status in listOf(400, 401) && refusal.serverCode != "SIGN_IN_FAILED") {
            Result.failure(AppError.SignInFailed())
        } else {
            result
        }
    }

    override suspend fun signOut(): Result<Unit> {
        val sessionId = store.onIo {
            clearPending()
            sessionId()
        } ?: return Result.success(Unit)
        return try {
            apiResult { api.deleteSession().bodyOrError().map { } }
        } finally {
            // Also when the caller gave up: the user asked to be signed out.
            withContext(NonCancellable) { store.onIo { clearSessionIfCurrent(sessionId) } }
        }
    }

    override suspend fun clear() = store.onIo {
        clearPending()
        clearSession()
    }

    // The cause is dropped: a parser message quotes the input, which may hold the session id.
    private inline fun <reified T> parse(body: String): T =
        runCatching { JSON_FORMAT.decodeFromString<T>(body) }.getOrNull() ?: throw AppError.Parse()

    @Serializable
    private class RequestTokenAnswer(
        @SerialName("request_token") val requestToken: String,
        @SerialName("sign_in_secret") val signInSecret: String,
        @SerialName("expires_at") val expiresAt: String? = null,
        @SerialName("approve_url") val approveUrl: String
    )

    @Serializable
    private class SessionAnswer(
        @SerialName("session_id") val sessionId: String,
        val account: AccountAnswer
    )

    @Serializable
    private class AccountAnswer(val id: JsonPrimitive, val username: String = "")

    private companion object {
        const val APPROVAL_HOST = "www.themoviedb.org"
        val JSON = "application/json".toMediaType()
        val JSON_FORMAT = Json { ignoreUnknownKeys = true }
        val HEADER_SAFE = Regex("[\\x21-\\x7E]+")

        /** Used when `expires_at` cannot be read. The server enforces the real limit. */
        val DEFAULT_LIFETIME: Duration = Duration.ofMinutes(15)
    }
}
