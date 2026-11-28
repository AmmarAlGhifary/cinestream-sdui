package com.ammar.network.session

/** The signed-in TMDB account. The session id is deliberately not part of it. */
data class Account(val id: String, val username: String)

sealed interface SessionState {
    data object SignedOut : SessionState
    data class SignedIn(val account: Account) : SessionState
}
