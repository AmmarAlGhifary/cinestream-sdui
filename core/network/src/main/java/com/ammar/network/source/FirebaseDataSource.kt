package com.ammar.network.source

import kotlinx.coroutines.flow.Flow

interface FirebaseDataSource{
    fun getScreenBlueprint(screenId: String): Flow<Result<String>>
}