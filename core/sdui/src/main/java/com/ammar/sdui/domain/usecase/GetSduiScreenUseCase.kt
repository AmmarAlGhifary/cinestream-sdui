package com.ammar.sdui.domain.usecase

import com.ammar.sdui.domain.repository.SduiRepository
import javax.inject.Inject

class GetSduiScreenUseCase @Inject constructor(
    private val repository: SduiRepository
) {
    operator fun invoke(screenID: String) = repository.getScreen(screenID)
}