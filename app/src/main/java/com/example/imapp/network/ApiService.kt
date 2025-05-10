package com.example.imapp.network

import com.example.imapp.data.LoginRequest
import com.example.imapp.data.LoginResponse
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("api/auth/login/")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>


    @POST("api/ai/coze/non_stream")
    suspend fun requestAiReply(@Body body: RequestBody): Response<ResponseBody>

    @POST("api/tts/")
    suspend fun synthesizeTts(@Body body: RequestBody): Response<ResponseBody>
}
