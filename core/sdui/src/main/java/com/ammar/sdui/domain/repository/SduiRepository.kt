package com.ammar.sdui.domain.repository

import com.ammar.sdui.domain.model.SduiScreen
import kotlinx.coroutines.flow.Flow

interface SduiRepository {
    fun getScreen(screenId: String): Flow<Result<SduiScreen>>
}