package com.example.imapp.repository

import android.content.Context

import android.util.Log
import com.example.imapp.data.LoginRequest
import com.example.imapp.network.ApiClient

object AuthRepository {
    private const val TAG = "AuthRepository"

    suspend fun simpleLogin(context: Context, username: String, password: String): Boolean {
        Log.d(TAG, "登录请求开始: username=$username")
        return try {
            val response = ApiClient.api.login(LoginRequest(username, password))
            Log.d(TAG, "登录响应状态码: ${response.code()}")

            if (response.isSuccessful) {
                val token = response.body()?.token.orEmpty()
                Log.d(TAG, "登录成功，返回的 token: $token")
                if (token.isNotBlank()) {
                    saveToken(context, token)
                    true
                } else {
                    Log.w(TAG, "登录响应中 token 为空")
                    false
                }
            } else {
                Log.w(TAG, "登录失败，状态码: ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "登录异常: ${e.message}", e)
            false
        }
    }

    fun saveToken(context: Context, token: String) {
        val prefs = context.getSharedPreferences("myapp_prefs", Context.MODE_PRIVATE)
        prefs.edit().putString("auth_token", token).apply()
        Log.d(TAG, "token 已保存到本地")
    }

    fun getToken(context: Context): String? {
        val prefs = context.getSharedPreferences("myapp_prefs", Context.MODE_PRIVATE)
        val token = prefs.getString("auth_token", null)
        Log.d(TAG, "获取本地 token: $token")
        return token
    }
}
