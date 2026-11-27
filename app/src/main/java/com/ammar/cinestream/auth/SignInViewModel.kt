package com.ammar.cinestream.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ammar.network.error.AppError
import com.ammar.network.error.toAppError
import com.ammar.network.session.SessionRepository
import com.ammar.network.session.SignInCallback
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
    private var completing: Job? = null
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

    /** Takes any link the app was opened with; only the sign-in link does something. */
    fun onLink(link: String) {
        if (SignInCallback.parse(link) == null || completing?.isActive == true) return
        _error.value = null
        completing = viewModelScope.launch {
            sessionRepository.completeSignIn(link).onFailure { failure ->
                fail(failure.toAppError()) { onLink(link) }
            }
        }
    }

    /** No browser could open the approval page. */
    fun onPageUnavailable() = fail(AppError.Unknown(), ::signIn)

    fun retry() = lastAttempt()

    fun dismissError() {
        _error.value = null
    }

    private fun fail(error: AppError, retry: () -> Unit) {
        lastAttempt = retry
        _error.value = error
    }
}
