package com.ammar.sdui.domain.usecase

import com.ammar.sdui.domain.model.ApiAction
import com.ammar.sdui.domain.repository.ActionRepository
import javax.inject.Inject

class SendApiActionUseCase @Inject constructor(
    private val repository: ActionRepository
) {
    suspend operator fun invoke(action: ApiAction) = repository.send(action)
}
