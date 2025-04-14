package com.example.imapp

import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.imapp.audio.AudioQueueManager
import com.example.imapp.network.ChatWebSocketListener
import com.example.imapp.network.Message
import com.example.imapp.network.WebSocketManager
import com.example.imapp.ui.theme.IMAppTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File

class MainActivity : ComponentActivity() {

    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 请求录音权限
        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                if (!granted) {
                    Log.e("MainActivity", "Record audio permission denied")
                }
            }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        // WebSocket消息流
        val maxMessages = 50
        val messageFlow = MutableStateFlow<List<Message>>(emptyList())

        // 监听WebSocket回调
        val webSocketListener = ChatWebSocketListener { message ->
            messageFlow.update { current -> (current + message).takeLast(maxMessages) }
        }
        WebSocketManager.setOnMessageReceivedCallback { message ->
            messageFlow.update { current -> (current + message).takeLast(maxMessages) }
        }
        // 连接 WebSocket
        WebSocketManager.connect(webSocketListener)

        // 启动预录音频的循环播放 (示例: 先准备几段文件路径, 你要根据实际情况放置)
        AudioQueueManager.startLoop(
            listOf(
                // 替换为你预录音频的真实本地路径
//                "${cacheDir}/pre_recorded_A.mp3",
//                "${cacheDir}/pre_recorded_B.mp3"
            )
        )

        setContent {
            IMAppTheme {
                HomeScreen(
                    messageFlow = messageFlow,
                    onSendText = { inputText ->
                        if (inputText.isNotBlank()) {
                            Log.d("MainActivity", "Send text: $inputText")
                            WebSocketManager.sendMessage(
                                Message.TextMessage(sender = "AndroidApp", text = inputText)
                            )
                        }
                    },
                    onStartRecord = { startRecording() },
                    onStopRecord = { stopRecording() },
                    onSendVoice = {
                        if (audioFilePath.isNotEmpty()) {
                            val file = File(audioFilePath)
                            val audioData = file.readBytes()
                            val encodedAudio = Base64.encodeToString(audioData, Base64.DEFAULT)
                            val duration = calculateAudioDuration(audioFilePath)
                            val audioMsg = Message.AudioMessage(
                                sender = "AndroidApp",
                                data = encodedAudio,
                                duration = duration,
                                format = "webm"
                            )
                            WebSocketManager.sendMessage(audioMsg)
                        }
                    },
                    onRequestAiReply = { pluginText ->
                        // 调用刚才写的 fetchAiReply
                        fetchAiReply(pluginText, messageFlow)
                    },
                    onRequestTts = { aiText ->
                        // 调用 fetchTtsAudio
                        fetchTtsAudio(aiText, messageFlow)
                    }
                )
            }
        }
    }

    private fun startRecording() {
        val file = File(cacheDir, "recorded_audio.webm")
        audioFilePath = file.absolutePath

        mediaRecorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(audioFilePath)
            prepare()
            start()
        }
    }


    private fun stopRecording() {
        try {
            // 测试发现录音时间太短会抛异常
            mediaRecorder?.let {
                it.stop()    // 这里可能抛出 IllegalStateException
                it.release() // 释放资源
            }
        } catch (e: IllegalStateException) {
            Log.e("MainActivity", "录音停止时发生IllegalStateException: ${e.message}", e)
        } catch (e: RuntimeException) {
            Log.e("MainActivity", "录音停止时发生RuntimeException: ${e.message}", e)
        } finally {
            mediaRecorder = null
        }
    }


    private fun calculateAudioDuration(filePath: String): Int {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(filePath)
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()
        return (durationStr?.toInt() ?: 0) / 1000
    }

    private fun fetchAiReply(pluginQuestion: String, messageFlow: MutableStateFlow<List<Message>>) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val jsonObj = JSONObject().apply {
                    put("user_id", "peter")
                    put("message", pluginQuestion)
                }
                val body = jsonObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url("https://beforeai.net/api/ai/coze/non_stream")
                    .post(body)
                    .build()

                val response = OkHttpClient().newCall(request).execute()
                val rspStr = response.body?.string().orEmpty()
                val rspJson = JSONObject(rspStr)
                val replyObj = rspJson.optJSONObject("reply")
                val replyContent = replyObj?.optString("content") ?: "AI回复解析失败"

                // 回到主线程更新 UI
                withContext(Dispatchers.Main) {
                    val aiMsg = Message.TextMessage(sender = "AI", text = replyContent)
                    messageFlow.update { current -> (current + aiMsg).takeLast(50) }
                }
            } catch (e: Exception) {
                Log.e("fetchAiReply", "异常: ${e.message}")
            }
        }
    }

    private fun fetchTtsAudio(aiText: String, messageFlow: MutableStateFlow<List<Message>>) {
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val jsonObj = JSONObject().apply {
                    put("text", aiText)
                    put("voice_id", "longwan")  // 固定 voice_id
                }
                val body = jsonObj.toString().toRequestBody("application/json; charset=utf-8".toMediaType())
                val request = Request.Builder()
                    .url("https://beforeai.net/api/tts/")
                    .post(body)
                    .build()

                val response = OkHttpClient().newCall(request).execute()
                val rspStr = response.body?.string().orEmpty()
                val rspJson = JSONObject(rspStr)

                if (rspJson.optBoolean("success", false)) {
                    val audioBase64 = rspJson.optString("audio_base64", "")

                    // 可选：这里你可以加一段逻辑来计算音频长度（duration）
                    val audioMessage = Message.AudioMessage(
                        sender = "AI",
                        data = audioBase64,
                        duration = 0,  // 如可计算可更新
                        format = "mp3"
                    )

                    // 回到主线程更新 UI
                    withContext(Dispatchers.Main) {
                        messageFlow.update { current -> (current + audioMessage).takeLast(50) }
                    }
                } else {
                    Log.w("fetchTtsAudio", "TTS 请求失败：${rspJson.optString("message")}")
                }
            } catch (e: Exception) {
                Log.e("fetchTtsAudio", "异常：${e.message}")
            }
        }
    }
}
