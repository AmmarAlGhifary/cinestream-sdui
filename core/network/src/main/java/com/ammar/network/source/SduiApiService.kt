package com.ammar.cinestream.core.network.source

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path

interface SduiApiService {
    
    @GET("api/{screenId}")
    suspend fun getScreenBlueprint(
        @Path("screenId") screenId: String
    ): Response<String>
    
}
