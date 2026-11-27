package com.ammar.cinestream

import com.ammar.network.session.Account
import com.ammar.network.session.SessionRepository
import com.ammar.network.session.SessionState
import com.ammar.sdui.domain.model.ActionResult
import com.ammar.sdui.domain.model.ApiAction
import com.ammar.sdui.domain.repository.ActionRepository
import com.ammar.sdui.domain.repository.SduiRepository
import com.ammar.sdui.presentation.registry.CineStreamBlueprint
import io.github.ammaralghifary.blueprint.model.SduiScreen
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow

/** Answers every screen request with [next] and records the parameters. */
class FakeRepository(var next: Result<SduiScreen>) : SduiRepository {
    val requests = mutableListOf<Map<String, String>>()

    override fun getScreen(screenId: String, params: Map<String, String>) = flow {
        requests += params
        emit(next)
    }
}

class FakeActions(var answer: suspend () -> Result<ActionResult> = { Result.success(ActionResult()) }) :
    ActionRepository {
    val sent = mutableListOf<ApiAction>()

    override suspend fun send(action: ApiAction): Result<ActionResult> {
        sent += action
        return answer()
    }
}

class FakeSessions : SessionRepository {
    override val session = MutableStateFlow<SessionState>(SessionState.SignedOut)
    override val signIns = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    var start: Result<String> = Result.success("https://tmdb.example/approve")
    var complete: Result<Account> = Result.success(Account("7", "tester"))
    var starts = 0
    val links = mutableListOf<String>()

    override suspend fun startSignIn(): Result<String> {
        starts++
        return start
    }

    override suspend fun completeSignIn(link: String): Result<Account> {
        links += link
        return complete.onSuccess { signIns.tryEmit(Unit) }
    }

    override suspend fun signOut() = Result.success(Unit)

    override fun clear() = Unit
}

fun screen(body: String, toolbar: String? = null): Result<SduiScreen> = Result.success(
    CineStreamBlueprint.instance.decodeScreen("""{"screen_id":"x","body":$body}""").let { decoded ->
        if (toolbar == null) decoded
        else decoded.copy(
            body = com.ammar.sdui.domain.model.SduiScaffold(
                toolbar = CineStreamBlueprint.instance.decodeScreen("""{"screen_id":"t","body":$toolbar}""").body,
                body = decoded.body
            )
        )
    }
)
