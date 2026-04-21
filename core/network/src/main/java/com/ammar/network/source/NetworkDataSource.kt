    package com.ammar.network.source

    import kotlinx.coroutines.flow.Flow

    interface NetworkDataSource {
        fun getScreenBlueprint(
            screenId: String,
            queryParams: Map<String, String> = emptyMap()
        ): Flow<Result<String>>
    }