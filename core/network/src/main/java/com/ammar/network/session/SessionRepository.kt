package com.ammar.network.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** TMDB sign-in. Every failure is an AppError. */
interface SessionRepository {

    val session: StateFlow<SessionState>

    /** Emits after each sign-in that succeeds, so screens can load again as the signed-in user. */
    val signIns: Flow<Unit>

    /** Asks for a request token and returns the page where the user approves it. */
    suspend fun startSignIn(): Result<String>

    /**
     * Finishes sign-in from the deep link. Fails with AppError.SignInFailed unless the link approves
     * the token this app asked for and that token has not expired.
     */
    suspend fun completeSignIn(link: String): Result<Account>

    /** Ends the session on the server. The stored session is removed even if that call fails. */
    suspend fun signOut(): Result<Unit>

    /** Forgets the stored session and any sign-in in progress, without calling the server. */
    fun clear()
}
