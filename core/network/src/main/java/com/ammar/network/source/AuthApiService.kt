package com.ammar.network.source

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST

internal interface AuthApiService {

    @POST("api/auth/request_token")
    suspend fun requestToken(): Response<ResponseBody>

    @POST("api/auth/session")
    suspend fun createSession(@Body body: RequestBody): Response<ResponseBody>

    @DELETE("api/auth/session")
    suspend fun deleteSession(): Response<ResponseBody>
}
