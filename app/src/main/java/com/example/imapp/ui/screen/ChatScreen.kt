package com.example.imapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.imapp.network.Message
import kotlinx.coroutines.flow.StateFlow

/**
 * ChatScreen 显示聊天消息和输入区域
 *
 * @param messageFlow 聊天消息流，包含 TextMessage、AudioMessage、SystemMessage 等
 * @param onSendText 发送文本消息回调
 * @param onStartRecord 开始录音回调（语音模式下触发）
 * @param onStopRecord 停止录音回调（可扩展，用于长按录音实现）
 * @param onSendVoice 发送语音消息回调
 */
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    messageFlow: StateFlow<List<Message>>,
    onSendText: (String) -> Unit,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onSendVoice: () -> Unit
) {
    val messages by messageFlow.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
    ) {
        // 消息列表区域
        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (messages.isEmpty()) {
                // 没有消息时，显示占位提示
                Text(
                    text = "暂无消息",
                    modifier = Modifier.align(Alignment.Center),
                    color = Color.Gray
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    items(messages) { msg ->
                        Spacer(modifier = Modifier.height(4.dp))
                        when (msg) {
                            is Message.TextMessage -> {
                                // 根据发送者决定对齐方式和样式
                                if (msg.sender == "AndroidApp") {
                                    // 用户自己发送的消息：右对齐、蓝色背景
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(4.dp),
                                        contentAlignment = Alignment.CenterEnd
                                    ) {
                                        Surface(
                                            tonalElevation = 2.dp,
                                            shape = MaterialTheme.shapes.medium,
                                            color = Color(0xFFD0E8FF)  // 浅蓝色背景
                                        ) {
                                            Text(
                                                text = "我: ${msg.text}",
                                                modifier = Modifier.padding(8.dp),
                                                color = Color.Black
                                            )
                                        }
                                    }
                                } else {
                                    // 其他消息（如来自插件或AI）：左对齐、灰色背景
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(4.dp),
                                        contentAlignment = Alignment.CenterStart
                                    ) {
                                        Surface(
                                            tonalElevation = 2.dp,
                                            shape = MaterialTheme.shapes.medium,
                                            color = Color(0xFFF0F0F0)  // 淡灰色背景
                                        ) {
                                            Column(modifier = Modifier.padding(8.dp)) {
                                                Text(
                                                    text = "${msg.sender}: ${msg.text}",
                                                    color = Color.Black
                                                )
                                                // 针对AI消息，显示“TTS”按钮
                                                if (msg.sender == "AI") {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Button(
                                                        onClick = {
                                                            // TODO: 调用TTS逻辑：生成语音并加入音频队列
                                                        },
                                                        modifier = Modifier.height(32.dp)
                                                    ) {
                                                        Text("朗读")
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            is Message.AudioMessage -> {
                                // 音频消息显示（例如来自语音合成或录音）
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Surface(
                                        tonalElevation = 2.dp,
                                        shape = MaterialTheme.shapes.medium,
                                        color = Color(0xFFE0FFE0)  // 淡绿色背景
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "Audio from ${msg.sender}, ${msg.duration}s",
                                                color = Color.Black
                                            )
                                            Button(
                                                onClick = {
                                                    // TODO: 实现播放语音逻辑（调用 AudioQueueManager 播放队列）
                                                },
                                                modifier = Modifier.height(32.dp)
                                            ) {
                                                Text("播放")
                                            }
                                        }
                                    }
                                }
                            }
                            is Message.SystemMessage -> {
                                // 系统消息显示
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(4.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "系统: ${msg.info}",
                                        color = Color.Red
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        // 分隔线
        Divider(color = Color.LightGray)
        // 底部输入区域（类似微信风格，带模式切换）
        ChatInputArea(
            onSendText = onSendText,
            onStartRecord = onStartRecord,
            onStopRecord = onStopRecord,
            onSendVoice = onSendVoice
        )
    }
}

/** 预览函数：在 Android Studio 中预览 ChatScreen */
@Preview(showBackground = true, name = "ChatScreenPreview")
@Composable
fun ChatScreenPreview() {
    // 构造模拟消息列表用于预览
    val mockMessages = listOf(
        Message.TextMessage(id = "1", timestamp = System.currentTimeMillis(), sender = "AndroidApp", receiver = "all", text = "这是我发的消息"),
        Message.TextMessage(id = "2", timestamp = System.currentTimeMillis(), sender = "ChromePlugin", receiver = "all", text = "你好，我是插件发的消息"),
        Message.TextMessage(id = "3", timestamp = System.currentTimeMillis(), sender = "AI", receiver = "all", text = "这是AI的回复")
    )
    val previewFlow = remember { androidx.compose.runtime.mutableStateOf(mockMessages) }
    // 为简化起见，使用 MutableStateFlow 实例包装预览数据
    val messagesFlow = kotlinx.coroutines.flow.MutableStateFlow<List<Message>>(mockMessages)

    MaterialTheme {
        ChatScreen(
            messageFlow = messagesFlow,
            onSendText = {},
            onStartRecord = {},
            onStopRecord = {},
            onSendVoice = {}
        )
    }
}
