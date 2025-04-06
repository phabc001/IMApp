package com.example.imapp

import android.Manifest
import android.content.pm.PackageManager
import android.media.*
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.example.imapp.audio.AudioQueueManager
import com.example.imapp.network.ChatWebSocketListener
import com.example.imapp.network.Message
import com.example.imapp.network.WebSocketManager
import com.example.imapp.ui.theme.IMAppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
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
                    onRecordStart = { startRecording() },
                    onRecordStop = { stopRecording() },
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
        mediaRecorder?.apply {
            stop()
            release()
        }
        mediaRecorder = null
    }

    private fun calculateAudioDuration(filePath: String): Int {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(filePath)
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()
        return (durationStr?.toInt() ?: 0) / 1000
    }
}
