package com.ammar.network.source

import kotlinx.serialization.json.JsonObject

/** Sends the request a server-driven action describes. */
interface ActionDataSource {

    /** The text of a 2xx answer. A failure is always an AppError. */
    suspend fun send(method: String, endpoint: String, body: JsonObject?): Result<String>
}
