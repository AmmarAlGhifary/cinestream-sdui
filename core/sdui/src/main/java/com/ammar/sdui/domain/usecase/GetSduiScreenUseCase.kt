package com.ammar.sdui.domain.usecase

import com.ammar.sdui.domain.repository.SduiRepository
import javax.inject.Inject

class GetSduiScreenUseCase @Inject constructor(
    private val repository: SduiRepository
) {
    operator fun invoke(
        screenID: String,
        params: Map<String, String> = emptyMap()
    ) = repository.getScreen(screenID, params)
}