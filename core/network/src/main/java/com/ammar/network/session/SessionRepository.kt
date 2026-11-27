package com.ammar.network.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** What a sign-in link led to, when it did not fail. */
sealed interface SignInOutcome {
    data class SignedIn(val account: Account) : SignInOutcome

    /** The link does not belong to a sign-in in progress. Nothing changed and nothing is shown. */
    data object Ignored : SignInOutcome
}

/** TMDB sign-in. Every failure is an AppError. */
interface SessionRepository {

    val session: StateFlow<SessionState>

    /**
     * Emits after each sign-in and sign-out the user asked for, so screens can load again. It does
     * not emit when the backend ends a session: the screen that learns of it says so itself.
     */
    val accountChanges: Flow<Unit>

    /** Asks for a request token and returns the page where the user approves it. */
    suspend fun startSignIn(): Result<String>

    /**
     * Finishes sign-in from the deep link. Any app can send such a link, so one without the token
     * this app is waiting for is ignored. Fails with AppError.SignInFailed if the link carries that
     * token but declines it, or the token has expired.
     */
    suspend fun completeSignIn(link: String): Result<SignInOutcome>

    /**
     * Ends the session on the server. The stored session is removed, and [accountChanges] emits, even
     * if that call fails.
     */
    suspend fun signOut(): Result<Unit>

    /** Forgets the stored session and any sign-in in progress, without calling the server. */
    suspend fun clear()
}
