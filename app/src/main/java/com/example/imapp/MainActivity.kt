//MainActivity.kt
package com.example.imapp

import android.Manifest
import android.content.pm.PackageManager
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.imapp.network.WebSocketManager
import com.example.imapp.ui.theme.IMAppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items


import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.material3.ButtonDefaults
import com.example.imapp.network.Message
import com.example.imapp.network.ChatWebSocketListener

import java.io.File
import android.util.Base64




class MainActivity : ComponentActivity() {

    private var mediaRecorder: MediaRecorder? = null
    private var mediaPlayer: MediaPlayer? = null
    private var audioFilePath: String = ""
    private var isAutoPlayEnabled by mutableStateOf(true) // 自动播放状态


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 检查录音权限
        val requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (!isGranted) {
                    Log.e("IMApp", "Record audio permission denied")
                }
            }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }

        // 用于存储接收到的消息，先进先出队列
        val maxMessages = 10 // 最大消息数量
        val messageFlow = MutableStateFlow<List<Message>>(emptyList()) // 不可变列表


        // 初始化 WebSocket
        val webSocketListener = ChatWebSocketListener { message ->
            messageFlow.update { currentMessages ->
                // 保持消息队列最多 maxMessages 条
                (currentMessages + message).takeLast(maxMessages)
            }
        }

        WebSocketManager.setOnMessageReceivedCallback { message ->
            messageFlow.update { currentMessages ->
                (currentMessages + message).takeLast(maxMessages)
            }
        }


        WebSocketManager.connect(webSocketListener)

        setContent {
            IMAppTheme {
                WebSocketScreen(messageFlow,
                    onSendText = {text ->onSendText(text)
                    },
                    onSendAudio = {
                        if (audioFilePath.isNotEmpty()) {
                            val file = File(audioFilePath)
                            val audioData = file.readBytes()
                            val encodedAudio = Base64.encodeToString(audioData, Base64.DEFAULT)
                            val duration = calculateAudioDuration(audioFilePath) // 计算音频时长
                            val message = Message.AudioMessage(
                                sender = "AndroidApp",
                                data = encodedAudio,
                                duration = duration, // 假设音频时长为 5 秒
                                format = "webm"
                            )
                            WebSocketManager.sendMessage(message)
                        }
                    },
                    onRecordStart = { startRecording() },
                    onRecordStop = { stopRecording() },
                    onPlayAudio = { encodedData ->
                        playAudio(encodedData)
                    },
                    isAutoPlayEnabled = isAutoPlayEnabled,
                    onToggleAutoPlay = { isAutoPlayEnabled = !isAutoPlayEnabled } // 切换状态
                )
            }
        }
    }

    private fun onSendText(inputMessage: String) {
        if (inputMessage.isNotBlank()) {
            Log.d("WebSocketScreen", "Sending message: $inputMessage")
            WebSocketManager.sendMessage(
                Message.TextMessage(sender = "AndroidApp", text = inputMessage)
            )
        } else {
            Log.e("WebSocketScreen", "Message is blank, not sending")
        }
    }

    /**
     * 使用 MediaMetadataRetriever 计算音频时长
     * @param filePath 音频文件路径
     * @return 时长（秒）
     */
    private fun calculateAudioDuration(filePath: String): Int {
        val retriever = MediaMetadataRetriever()
        retriever.setDataSource(filePath)
        val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
        retriever.release()
        return (durationStr?.toInt() ?: 0) / 1000 // 毫秒转秒
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

    private fun playAudio(encodedData: String) {
        val audioData = Base64.decode(encodedData, Base64.DEFAULT)
        val tempFile = File.createTempFile("temp_audio", ".webm", cacheDir)
        tempFile.writeBytes(audioData)
        mediaPlayer = MediaPlayer().apply {
            setDataSource(tempFile.absolutePath)
            prepare()
            start()
        }
    }
}



@Composable
fun WebSocketScreen(messageFlow: MutableStateFlow<List<Message>>,
                    onSendText: (String) -> Unit,
                    onSendAudio: () -> Unit,
                    onRecordStart: () -> Unit,
                    onRecordStop: () -> Unit,
                    onPlayAudio: (String) -> Unit,
                    isAutoPlayEnabled: Boolean,
                    onToggleAutoPlay: () -> Unit) {
    val messages by messageFlow.collectAsState() // 观察消息流的变化
    var inputMessage by remember { mutableStateOf("") } // 用于存储用户输入

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // 显示消息列表
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ){
            items(messages.toList()) { message -> // 转为 List 以供 LazyColumn 使用
                when (message) {
                    is Message.TextMessage -> {
                        Text(
                            text = "Text: ${message.text}",
                            fontSize = 16.sp,
                            color = Color.Black,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                    is Message.AudioMessage -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Audio: ${message.duration}s",
                                fontSize = 16.sp,
                                color = Color.Gray,
                                modifier = Modifier.padding(vertical = 4.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(onClick = { onPlayAudio(message.data) }) {
                                Text("Play")
                            }
                            if (isAutoPlayEnabled) {
                                LaunchedEffect(message.data) {
                                    onPlayAudio(message.data)
                                }
                            }
                        }
                    }
                    is Message.SystemMessage -> {
                        Text(
                            text = "System: ${message.info}",
                            fontSize = 14.sp,
                            color = Color.Red,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }

        // 输入框和发送按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = inputMessage,
                onValueChange = { inputMessage = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
                    .height(50.dp)
                    .background(Color.LightGray) // 设置背景色，避免和页面背景一致
                    .padding(8.dp), // 内边距


                maxLines = 1,
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Blue,
                    contentColor = Color.White
                ),
                onClick = {
                    onSendText(inputMessage)
                    inputMessage = ""
                }
            ) {
                Text("Send")
            }

        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Button(
                onClick = onRecordStart,
                modifier = Modifier.weight(1f) // 每个按钮分配相等的空间
            ) {
                Text("Start Recording")
            }

            Spacer(modifier = Modifier.width(8.dp)) // 按钮之间的间隔
            Button(
                onClick = onRecordStop,
                modifier = Modifier.weight(1f)
            ) {
                Text("Stop Recording")
            }
            Spacer(modifier = Modifier.width(8.dp)) // 按钮之间的间隔
            Button(onClick = onSendAudio,
                modifier = Modifier.weight(1f)
            ) {
                Text("Send Audio")
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 自动播放开关
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Auto Play Audio",
                fontSize = 16.sp,
                modifier = Modifier.padding(start = 8.dp)
            )
            Button(
                onClick = onToggleAutoPlay,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isAutoPlayEnabled) Color.Green else Color.Red
                )
            ) {
                Text(if (isAutoPlayEnabled) "ON" else "OFF")
            }
        }

    }
}


