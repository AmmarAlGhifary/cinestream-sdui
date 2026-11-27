package com.ammar.network.session

import com.ammar.network.error.AppError
import com.ammar.network.source.AuthApiService
import com.ammar.network.source.apiResult
import com.ammar.network.source.bodyOrError
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
        store.clearPending()
        api.requestToken().bodyOrError().mapCatching { body ->
            val answer = parse<RequestTokenAnswer>(body)
            // The page gets the user's TMDB password, so only a secure address is opened.
            if (answer.requestToken.isBlank() || answer.signInSecret.isBlank() ||
                answer.approveUrl.toHttpUrlOrNull()?.isHttps != true
            ) {
                throw AppError.Parse()
            }
            val expiresAt = parseExpiry(answer.expiresAt) ?: clock.instant().plus(DEFAULT_LIFETIME)
            store.savePending(PendingSignIn(answer.requestToken, answer.signInSecret, expiresAt.toEpochMilli()))
            answer.approveUrl
        }
    }

    override suspend fun completeSignIn(link: String): Result<SignInOutcome> = completing.withLock {
        val callback = SignInCallback.parse(link)
        val token = callback?.requestToken
        store.dropExpiredPending()
        if (token != null && store.takeLapsed(token)) {
            return@withLock Result.failure(AppError.SignInFailed())
        }
        val pending = store.pending()
        // A link that does not carry our token must neither cancel the real sign-in nor bother anyone.
        if (pending == null || token != pending.requestToken) {
            return@withLock Result.success(SignInOutcome.Ignored)
        }
        if (callback?.approved != true) {
            store.clearPending()
            return@withLock Result.failure(AppError.SignInFailed())
        }
        createSession(pending)
    }

    private suspend fun createSession(pending: PendingSignIn): Result<SignInOutcome> = apiResult {
        val body = buildJsonObject {
            put("request_token", pending.requestToken)
            put("sign_in_secret", pending.secret)
        }.toString()
        api.createSession(body.toRequestBody(JSON)).bodyOrError().mapCatching { answer ->
            val created = parse<SessionAnswer>(answer)
            // It becomes a header value, so anything but visible ASCII is rejected here.
            if (!HEADER_SAFE.matches(created.sessionId)) throw AppError.Parse()
            val account = Account(created.account.id.content, created.account.username)
            store.save(StoredSession(created.sessionId, account.id, account.username))
            store.clearPending()
            _signIns.tryEmit(Unit)
            SignInOutcome.SignedIn(account) as SignInOutcome
        }
    }.recoverCatching { error ->
        if (error is AppError.Http && error.status in 400..499 && error.status != 408 && error.status != 429) {
            // A refused token cannot be used again; after a network or server failure it still can.
            store.clearPending()
            // To the user a refused sign-in is a failed one, whatever the backend objected to.
            if (error.status in listOf(400, 401) && error.serverCode != "SIGN_IN_FAILED") {
                throw AppError.SignInFailed()
            }
        }
        throw error
    }

    override suspend fun signOut(): Result<Unit> {
        val sessionId = store.sessionId()
        store.clearPending()
        if (sessionId == null) return Result.success(Unit)
        return try {
            apiResult { api.deleteSession().bodyOrError().map { } }
        } finally {
            store.clearSessionIfCurrent(sessionId)
        }
    }

    override fun clear() {
        store.clearPending()
        store.clearSession()
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
        val JSON = "application/json".toMediaType()
        val JSON_FORMAT = Json { ignoreUnknownKeys = true }
        val HEADER_SAFE = Regex("[\\x21-\\x7E]+")

        /** Used when `expires_at` cannot be read. The server enforces the real limit. */
        val DEFAULT_LIFETIME: Duration = Duration.ofMinutes(15)
    }
}
