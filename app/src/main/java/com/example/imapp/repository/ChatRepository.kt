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
import com.example.imapp.model.AiReplyRequest
import com.example.imapp.model.TtsRequest
import com.example.imapp.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.util.UUID

object ChatRepository {
    // 保存最近的聊天消息列表（最多50条）
    private const val MAX_MESSAGES = 50
    private val _messageFlow = MutableStateFlow<List<Message>>(emptyList())
    val messageFlow = _messageFlow  // 供外部（ViewModel/UI）读取的流

    private val _ttsFlow = MutableSharedFlow<AudioItem>(replay = 0)
    val ttsFlow = _ttsFlow.asSharedFlow()

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


    suspend fun requestAiReply(question: String) {
        try {
            val response = ApiClient.api.requestAiReply(
                AiReplyRequest(user_id = "peter", message = question)
            )
            if (response.isSuccessful) {
                val reply = response.body()?.reply?.content ?: "AI回复解析失败"
                val aiMsg = Message.TextMessage(sender = "AI", text = reply)
                onReceiveMessage(aiMsg)
            } else {
                Log.w("ChatRepository", "AI 回复失败: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "AI 回复异常: ${e.message}")
        }
    }

    suspend fun requestTtsAudio(ctx: Context, text: String) {
        try {
            val response = ApiClient.api.synthesizeTts(
                TtsRequest(text = text, voice_id = "longwan")
            )
            if (response.isSuccessful) {
                val rsp = response.body()
                if (rsp?.success == true && rsp.audio_base64.isNotBlank()) {
                    val audioData = Base64.decode(rsp.audio_base64, Base64.DEFAULT)
                    val dir = File(ctx.filesDir, "tts_audios").apply { mkdirs() }
                    val file = File(dir, "tts_${System.currentTimeMillis()}.mp3")
                    file.writeBytes(audioData)

                    val durationSec = calculateAudioDuration(file)
                    val ttsItem = AudioItem(
                        id = UUID.randomUUID().toString(),
                        name = file.name,
                        uri = file.absolutePath
                    )

                    withContext(Dispatchers.Default) {
                        _ttsFlow.emit(ttsItem)
                    }

                    val audioMsg = Message.AudioMessage(
                        sender = "AI",
                        data = rsp.audio_base64,
                        duration = durationSec,
                        format = "mp3"
                    )
                    onReceiveMessage(audioMsg)
                } else {
                    Log.w("ChatRepository", "TTS接口返回无效")
                }
            } else {
                Log.w("ChatRepository", "TTS接口失败: ${response.code()}")
            }
        } catch (e: Exception) {
            Log.e("ChatRepository", "请求 TTS 异常: ${e.message}")
        }
    }


    private fun calculateAudioDuration(file: File): Int {
        return try {
            val mmr = MediaMetadataRetriever().apply { setDataSource(file.absolutePath) }
            val durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            mmr.release()
            durationStr?.toIntOrNull()?.div(1000) ?: 0 // 转为秒
        } catch (e: Exception) {
            0
        }
    }

}
