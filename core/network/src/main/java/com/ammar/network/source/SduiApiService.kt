package com.ammar.cinestream.core.network.source

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.QueryMap

interface SduiApiService {
    
    @GET("api/{screenId}")
    suspend fun getScreenBlueprint(
        @Path("screenId") screenId: String,
        @QueryMap queryParams: Map<String, String> = emptyMap()
    ): Response<ResponseBody>

}
