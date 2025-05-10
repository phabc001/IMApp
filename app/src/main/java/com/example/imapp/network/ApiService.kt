package com.example.imapp.network

import com.example.imapp.data.LoginRequest
import com.example.imapp.data.LoginResponse
import com.example.imapp.model.AiReplyRequest
import com.example.imapp.model.AiReplyResponse
import com.example.imapp.model.TtsRequest
import com.example.imapp.model.TtsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {

    @POST("api/auth/login/")
    suspend fun login(@Body body: LoginRequest): Response<LoginResponse>

    @POST("api/ai/coze/non_stream")
    suspend fun requestAiReply(@Body body: AiReplyRequest): Response<AiReplyResponse>

    @POST("api/tts/")
    suspend fun synthesizeTts(@Body body: TtsRequest): Response<TtsResponse>

}
