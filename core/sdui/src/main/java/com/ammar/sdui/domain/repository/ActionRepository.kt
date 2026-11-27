package com.ammar.sdui.domain.repository

import com.ammar.sdui.domain.model.ActionResult
import com.ammar.sdui.domain.model.ApiAction

interface ActionRepository {

    /** Sends [action] to the backend. A failure is always an AppError. */
    suspend fun send(action: ApiAction): Result<ActionResult>
}
