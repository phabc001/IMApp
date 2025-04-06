package com.example.imapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.imapp.network.Message

@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    messageFlow: MutableStateFlow<List<Message>>,
    onSendText: (String) -> Unit,
    onRecordStart: () -> Unit,
    onRecordStop: () -> Unit,
    onSendVoice: () -> Unit
) {
    val messages by messageFlow.collectAsState()
    var inputText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        if (messages.isEmpty()) {

            Text("暂无消息")

        } else {
            // 消息列表
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.White)
            ) {
                items(messages) { msg ->
                    Spacer(modifier = Modifier.height(4.dp))
                    when (msg) {
                        is Message.TextMessage -> {
                            // 简单文本显示
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFE0E0E0))
                                    .padding(8.dp)
                            ) {
                                Text(text = "${msg.sender}: ${msg.text}")

                                // 当sender不是自己且不是AI时，可显示"AI Reply"按钮
                                if (msg.sender != "AndroidApp" && msg.sender != "AI") {
                                    Button(onClick = {
                                        // 调用AI API -> new AI text message
                                        // 你可以在MainActivity里写个 onAiReply(msg) 回调
                                        // 然后这里调用之
                                    }) {
                                        Text("AI Reply")
                                    }
                                }

                                // 当sender是"AI"，可显示"TTS"按钮
                                if (msg.sender == "AI") {
                                    Button(onClick = {
                                        // 调用TTS -> audio -> 插播
                                    }) {
                                        Text("TTS")
                                    }
                                }
                            }
                        }

                        is Message.AudioMessage -> {
                            // 音频条
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFD0FFD0))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Audio from ${msg.sender}, ${msg.duration}s")
                                Button(onClick = {
                                    // 在现有代码中，你写了 MediaPlayer播放
                                    // 但最好改用 AudioQueueManager 或 直接 playAudio
                                }) {
                                    Text("Play")
                                }
                            }
                        }

                        is Message.SystemMessage -> {
                            Text("System: ${msg.info}", color = Color.Red)
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        }

        ChatInputArea(
            onSendText = onSendText,
            onStartRecord = onRecordStart,
            onStopRecord = onRecordStop,
            onSendVoice = onSendVoice
        )

    }
}



    /**
     * 预览函数：在 Android Studio 中查看 ChatScreen 的布局效果。
     * - 需在 Build 下有 "Compose Preview" 配置时才能正常预览
     * - 不会在实际运行时调用
     */
    @Preview(showBackground = true, name = "ChatScreenPreview")
    @Composable
    fun ChatScreenPreview() {
        // 构造一些模拟的消息数据
        val mockMessages = listOf(
            Message.TextMessage(sender = "AndroidApp", text = "Hello from me"),
            Message.TextMessage(sender = "ChromePlugin", text = "Hi, I'm from plugin"),
            Message.TextMessage(sender = "AI", text = "AI reply sample..."),
            Message.SystemMessage(info = "System maintenance alert")
        )
        // 使用 remember + MutableStateFlow 来生成预览用的 flow
        val previewFlow = remember { MutableStateFlow(mockMessages) }

        // 调用 ChatScreen，传入 mock 数据和空的回调
        ChatScreen(
            messageFlow = previewFlow,
            onSendText = {},
            onRecordStart = {},
            onRecordStop = {},
            onSendVoice = {}
        )
    }
