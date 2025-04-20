package com.example.imapp.repository

import com.example.imapp.data.Voice
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.*

object VoiceRepository {

    /* ---------- 本地默认音色 ---------- */
    private val defaultVoices = listOf(
        Voice("longwan",  "龙婉",  true),
        Voice("longcheng","龙橙",  true),
        Voice("longshuo", "龙硕",  true),
        Voice("longyuan", "龙媛",  true)
    )

    /* ---------- 网络配置 ---------- */
    private const val BASE = "https://your-django-site.com"      // TODO 替换为实际域名
    private val client = OkHttpClient()

    /* ---------- 列表状态 ---------- */
    private val _voices = MutableStateFlow(defaultVoices)
    val voices = _voices.asStateFlow()

    fun addTop(voice: Voice) { _voices.value = listOf(voice) + _voices.value }

    fun delete(id: String) {
        _voices.value = _voices.value.filterNot { !it.isDefault && it.voiceId == id }
    }

    /* =========================================================================
       1) 兼容旧方法：仅给 filePath -> 自动 upload + create
     * ========================================================================= */
    suspend fun createVoice(filePath: String): Result<Voice> =
        uploadAndCreate(File(filePath), "自定义音色")

    /* =========================================================================
       2) 核心：转码(可选) -> 上传到 OSS -> 调用 /voice/create -> 返回 Voice
     * ========================================================================= */
    suspend fun uploadAndCreate(local: File, displayName: String): Result<Voice> =
        withContext(Dispatchers.IO) {
            try {
                /* 1️⃣ 可选：本地转码，示例直接使用原文件 */
                val fileToSend = local

                /* 2️⃣ 上传 */
                val url = uploadFile(fileToSend)
                    ?: return@withContext Result.failure(RuntimeException("上传失败"))

                /* 3️⃣ 创建 voice */
                val voice = createVoice(displayName, url)
                    ?: return@withContext Result.failure(RuntimeException("创建失败"))

                addTop(voice)
                Result.success(voice)

            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /* ---------- 上传文件到后端，返回可公开访问的 URL ---------- */
    private fun uploadFile(file: File): String? {
        val reqBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart(
                name = "file",
                filename = file.name,
                body = file.asRequestBody("audio/*".toMediaTypeOrNull())
            )
            .build()

        val req = Request.Builder()
            .url("$BASE/api/upload/")
            .post(reqBody)
            .build()

        client.newCall(req).execute().use { rsp ->
            if (!rsp.isSuccessful) return null
            val url = JSONObject(rsp.body?.string() ?: "{}").optString("url", "")
            return if (url.isNotBlank()) url else null
        }
    }

    /* ---------- 调用 /api/voice/create/ 创建音色 ---------- */
    private fun createVoice(displayName: String, fileUrl: String): Voice? {
        val json = JSONObject().apply {
            put("display_name", displayName)
            put("file_url", fileUrl)
        }
        val reqBody = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val req = Request.Builder()
            .url("$BASE/api/voice/create/")
            .post(reqBody)
            .build()

        client.newCall(req).execute().use { rsp ->
            if (!rsp.isSuccessful) return null
            val body = JSONObject(rsp.body?.string() ?: "{}")
            val vid  = body.optString("voice_id", "")
            val name = body.optString("display_name", displayName)
            return if (vid.isNotBlank()) Voice(vid, name) else null
        }
    }
}
