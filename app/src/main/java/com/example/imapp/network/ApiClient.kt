package com.example.imapp.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import com.example.imapp.BuildConfig

object ApiClient {
    private const val BASE_URL = BuildConfig.API_BASE_URL

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val api: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }
}
