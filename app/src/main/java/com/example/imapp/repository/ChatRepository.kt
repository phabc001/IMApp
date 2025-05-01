// ChatRepository.kt
package com.example.imapp.repository

import android.content.Context
import com.example.imapp.network.Message
import com.example.imapp.network.WebSocketManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import android.util.Base64
import android.util.Log
import java.io.File
import android.media.MediaMetadataRetriever
import com.example.imapp.audio.AudioQueueManager
import com.example.imapp.data.AudioItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.util.UUID

object ChatRepository {
    // 保存最近的聊天消息列表（最多50条）
    private const val MAX_MESSAGES = 50
    private val _messageFlow = MutableStateFlow<List<Message>>(emptyList())
    val messageFlow = _messageFlow  // 供外部（ViewModel/UI）读取的流

    // 服务或WebSocket线程调用：收到新消息时更新消息流
    fun onReceiveMessage(message: Message) {
        _messageFlow.update { current ->
            (current + message).takeLast(MAX_MESSAGES)
        }
    }

    // 发送文本消息，通过 WebSocketManager
    fun sendTextMessage(text: String) {
        if (text.isBlank()) return
        val msg = Message.TextMessage(sender = "AndroidApp", text = text)
        WebSocketManager.sendMessage(msg)
        // 本地也先添加这条消息到列表
        onReceiveMessage(msg)
    }

    // 发送音频消息（在录音停止后调用）
    fun sendAudioMessage(audioFile: File) {
        if (!audioFile.exists()) return
        try {
            // 读取文件并编码为 Base64
            val audioBytes = audioFile.readBytes()
            val encodedAudio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
            // 可选：获取音频时长和格式（假设文件后缀或编码格式已知）
            val format = audioFile.extension.ifEmpty { "audio" }
            // 这里可以根据需要获取音频时长，略去详细实现
            val durationSec = calculateAudioDuration(audioFile)
            // 构造音频消息对象
            val message = Message.AudioMessage(
                sender = "AndroidClient",
                receiver = "all",
                data = encodedAudio,
                duration = durationSec,
                format = format
            )
            WebSocketManager.sendMessage(message)
            onReceiveMessage(message)
        } catch (e: Exception) {
            Log.e("ChatRepository", "发送音频消息失败: ${e.message}")
        }
    }

    // 请求 AI 对指定文本回复
    suspend fun requestAiReply(question: String) {
        try {
            val json = JSONObject().apply {
                put("user_id", "peter")
                put("message", question)
            }
            val body = json.toString().toRequestBody(contentType = "application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("https://beforeai.net/api/ai/coze/non_stream")
                .post(body)
                .build()
            val response = OkHttpClient().newCall(request).execute()
            val rspStr = response.body?.string().orEmpty()
            val rspJson = JSONObject(rspStr)
            val replyObj = rspJson.optJSONObject("reply")
            val replyContent = replyObj?.optString("content") ?: "AI回复解析失败"
            // 将 AI 回复作为新消息加入列表
            val aiMsg = Message.TextMessage(sender = "AI", text = replyContent)
            onReceiveMessage(aiMsg)
        } catch (e: Exception) {
            Log.e("ChatRepository", "AI 回复请求异常: ${e.message}")
        }
    }

    // 请求 TTS 合成语音并作为音频消息插入
    suspend fun requestTtsAudio(ctx: Context, text: String) {
        try {
            val json = JSONObject().apply {
                put("text", text)
                put("voice_id", "longwan")
            }
            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
            val request = Request.Builder()
                .url("https://beforeai.net/api/tts/")
                .post(body)
                .build()
            val response = OkHttpClient().newCall(request).execute()
            val rspStr = response.body?.string().orEmpty()
            val rspJson = JSONObject(rspStr)
            if (rspJson.optBoolean("success", false)) {
                val audioBase64 = rspJson.optString("audio_base64", "")
                val audioData = Base64.decode(audioBase64, Base64.DEFAULT)
                val dir = File(ctx.filesDir, "audios").apply { mkdirs() }
                val filename = "tts_${System.currentTimeMillis()}.mp3"
                val file = File(dir, filename)
                file.outputStream().use { it.write(audioData) }

                // 3. 构造 AudioItem
                val ttsItem = AudioItem(
                    id   = UUID.randomUUID().toString(),
                    name = filename,
                    uri  = file.absolutePath
                )

                // 在主线程初始化并操作播放器
                withContext(Dispatchers.Main) {
                        AudioQueueManager.init(ctx)
                        val current = AudioQueueManager.playingItem.value
                        if (AudioQueueManager.isPlaying && current != null) {
                                AudioQueueManager.insertAfter(listOf(ttsItem))
                            } else {
                               AudioQueueManager.playQueue(listOf(ttsItem), loop = false)
                            }
                    }


                val audioMsg = Message.AudioMessage(
                    sender = "AI",
                    data = audioBase64,
                    duration = 0,       // 可进一步计算时长
                    format = "mp3"
                )
                onReceiveMessage(audioMsg)
            } else {
                Log.w("ChatRepository", "TTS接口调用失败: ${rspJson.optString("message")}")
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "请求 TTS 异常: ${e.message}")
        }
    }

    // （可选）计算音频文件时长的工具方法
    private fun calculateAudioDuration(file: File): Int {
        // 简化处理：如有需要可使用MediaMetadataRetriever获取音频时长（毫秒），转换为秒
        // 这里先返回0占位
        return 0
    }
}
