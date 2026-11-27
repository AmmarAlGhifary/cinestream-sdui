package com.ammar.cinestream.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.network.error.AppError
import com.ammar.network.error.toAppError
import com.ammar.network.session.SessionRepository
import com.ammar.network.session.SignInCallback
import com.ammar.network.session.SignInOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Drives sign-in for the whole app. The screen the user came from reloads itself afterwards. */
@HiltViewModel
class SignInViewModel @Inject constructor(
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _error = MutableStateFlow<AppError?>(null)
    val error: StateFlow<AppError?> = _error.asStateFlow()

    private val pages = Channel<String>(Channel.CONFLATED)

    /** Approval pages to open in the browser, each delivered once. */
    val approvalPages: Flow<String> = pages.receiveAsFlow()

    private var starting: Job? = null
    private val linksInProgress = mutableSetOf<String>()
    private var resuming: Job? = null
    private var lastAttempt: () -> Unit = ::signIn

    fun signIn() {
        if (starting?.isActive == true) return
        _error.value = null
        starting = viewModelScope.launch {
            sessionRepository.startSignIn().fold(
                onSuccess = { pages.send(it) },
                onFailure = { fail(it.toAppError(), ::signIn) }
            )
        }
    }

    /** Takes any link the app was opened with; only the sign-in being waited for does something. */
    fun onLink(link: String) {
        // The same link twice is handled once. A different one must not be lost behind it.
        if (SignInCallback.parse(link) == null || !linksInProgress.add(link)) return
        viewModelScope.launch {
            try {
                sessionRepository.completeSignIn(link).fold(
                    onSuccess = { if (it is SignInOutcome.SignedIn) _error.value = null },
                    onFailure = { failure -> fail(failure.toAppError()) { onLink(link) } }
                )
            } finally {
                linksInProgress.remove(link)
            }
        }
    }

    /**
     * The app is in front again. A browser may have kept the redirect to itself, so a sign-in that
     * is still waiting gets one try. It stays silent unless it works.
     */
    fun onResume() {
        // The link, which arrives just before this, is already finishing the sign-in.
        if (linksInProgress.isNotEmpty() || resuming?.isActive == true) return
        resuming = viewModelScope.launch {
            sessionRepository.resumeSignIn().onSuccess {
                if (it is SignInOutcome.SignedIn) _error.value = null
            }
        }
    }

    /** No browser could open the approval page. */
    fun onPageUnavailable() = fail(AppError.Unknown(), ::signIn)

    fun retry() {
        _error.value = null
        lastAttempt()
    }

    fun dismissError() {
        _error.value = null
    }

    private fun fail(error: AppError, retry: () -> Unit) {
        lastAttempt = retry
        _error.value = error
    }
}
