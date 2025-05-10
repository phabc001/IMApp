package com.example.imapp.repository

import android.content.Context
import android.util.Base64
import android.util.Log
import com.example.imapp.data.AudioItem
import com.example.imapp.model.TtsRequest
import com.example.imapp.network.ApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

object TtsRepository {
    suspend fun synthesize(text: String, voiceId: String, ctx: Context): File? = withContext(Dispatchers.IO) {
        return@withContext try {
            val response = ApiClient.api.synthesizeTts(TtsRequest(text, voiceId))

            if (response.isSuccessful) {
                val body = response.body()
                if (body?.success == true && body.audio_base64.isNotBlank()) {
                    val audioBytes = Base64.decode(body.audio_base64, Base64.DEFAULT)
                    val file = File(ctx.getExternalFilesDir("audios"), "tts_${voiceId}_${UUID.randomUUID()}.mp3")
                    //val file = File(File(ctx.filesDir, "audios").apply { mkdirs() }, "tts_${voiceId}_${UUID.randomUUID()}.mp3")
                    file.writeBytes(audioBytes)

                    AudioRepository.addAudio(file)
                    file
                } else {
                    Log.w("TtsRepository", "TTS 返回内容无效: $body")
                    null
                }
            } else {
                Log.w("TtsRepository", "TTS 请求失败: ${response.code()}")
                null
            }
        } catch (e: Exception) {
            Log.e("TtsRepository", "TTS 请求异常: ${e.message}", e)
            null
        }
    }
}
