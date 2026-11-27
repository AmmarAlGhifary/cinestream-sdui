package com.ammar.sdui.domain.repository

import io.github.ammaralghifary.blueprint.model.SduiScreen
import kotlinx.coroutines.flow.Flow

interface SduiRepository {
    fun getScreen(
        screenId: String,
        params: Map<String, String> = emptyMap()
    ): Flow<Result<SduiScreen>>
}
