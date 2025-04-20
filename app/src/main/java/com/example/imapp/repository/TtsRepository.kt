package com.example.imapp.repository


import android.content.Context
import com.example.imapp.data.AudioItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okio.IOException
import java.io.File
import java.util.*
import com.example.imapp.repository.AudioRepository

object TtsRepository {
    private val client = OkHttpClient()

    /** 调用后端 TTS，返回生成的本地音频文件 */
    suspend fun synthesize(text: String, voiceId: String, ctx: Context): File? =
        withContext(Dispatchers.IO) {
            try {
                // TODO 替换为真实 TTS API
                val req = Request.Builder()
                    .url("https://example.com/tts?voice=$voiceId") // demo
                    .post(RequestBody.create(null, text))
                    .build()
                val rsp = client.newCall(req).execute()
                if (!rsp.isSuccessful) return@withContext null
                val bytes = rsp.body?.bytes() ?: return@withContext null
                val file = File(ctx.filesDir, "tts_${voiceId}_${UUID.randomUUID()}.mp3")
                file.writeBytes(bytes)
                AudioRepository.addAudio(file)       // 合成成功后加入音频库
                file
            } catch (e: IOException) { null }
        }
}
