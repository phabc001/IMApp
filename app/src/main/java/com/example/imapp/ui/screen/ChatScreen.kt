package com.example.imapp.ui.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.imapp.audio.AudioPlayerUtil
import com.example.imapp.network.Message
import kotlinx.coroutines.flow.StateFlow

/**
 * ChatScreen 显示聊天消息和输入区域
 *
 * @param messageFlow 聊天消息流，包含 TextMessage、AudioMessage、SystemMessage 等
 * @param onSendText 发送文本消息回调
 * @param onStartRecord 开始录音回调
 * @param onStopRecord 停止录音回调
 * @param onSendVoice 发送语音消息回调
 * @param onRequestAiReply 当用户长按插件消息后点“AI 回复”时触发，把消息文本传出去
 * @param onRequestTts 当用户点击“朗读”按钮时触发，把AI文本传出去
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    messageFlow: StateFlow<List<Message>>,
    onSendText: (String) -> Unit,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onSendVoice: () -> Unit,
    onRequestAiReply: (String) -> Unit,
    onRequestTts: (String) -> Unit
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
                                if (msg.sender == "AndroidApp") {
                                    // --------------- 用户自己发送的消息 ---------------
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
                                    // --------------- 插件或AI 发送的消息 ---------------
                                    // (1) 判断是否来自ChromePlugin
                                    val isPlugin = msg.sender == "ChromePlugin"
                                    // 用于长按菜单显隐
                                    var showMenu by remember { mutableStateOf(false) }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(4.dp)
                                            .combinedClickable(
                                                onClick = {},
                                                onLongClick = {
                                                    // (2) 仅当是插件消息时才弹出菜单
                                                    if (isPlugin) {
                                                        showMenu = true
                                                    }
                                                }
                                            ),
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
                                                // (3) 如果是AI消息，显示“朗读”按钮
                                                if (msg.sender == "AI") {
                                                    Spacer(modifier = Modifier.height(4.dp))
                                                    Button(
                                                        onClick = {
                                                            onRequestTts(msg.text) // 调用外部 TTS 回调
                                                        },
                                                        modifier = Modifier.height(32.dp)
                                                    ) {
                                                        Text("朗读")
                                                    }
                                                }
                                            }
                                        }

                                        // (4) 长按菜单 - 只在 showMenu=true 时出现
                                        if (showMenu) {
                                            DropdownMenu(
                                                expanded = showMenu,
                                                onDismissRequest = { showMenu = false }
                                            ) {
                                                DropdownMenuItem(
                                                    text = { Text("AI 回复") },
                                                    onClick = {
                                                        showMenu = false
                                                        // 回调出去，让上层调用 AI 接口
                                                        onRequestAiReply(msg.text)
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            is Message.AudioMessage -> {
                                // --------------- 音频消息 ---------------
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
                                            val context = LocalContext.current

                                            Button(
                                                onClick = {
                                                    AudioPlayerUtil.playBase64Audio(context, msg.data)
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
                                // --------------- 系统消息 ---------------
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
        // 底部输入区域
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
    val mockMessages = listOf(
        Message.TextMessage(
            id = "1",
            timestamp = System.currentTimeMillis(),
            sender = "AndroidApp",
            receiver = "all",
            text = "这是我发的消息"
        ),
        Message.TextMessage(
            id = "2",
            timestamp = System.currentTimeMillis(),
            sender = "ChromePlugin",
            receiver = "all",
            text = "你好，我是插件发的消息"
        ),
        Message.TextMessage(
            id = "3",
            timestamp = System.currentTimeMillis(),
            sender = "AI",
            receiver = "all",
            text = "这是AI的回复"
        )
    )
    val messagesFlow = remember { mutableStateOf(mockMessages) }
    ChatScreen(
        messageFlow = kotlinx.coroutines.flow.MutableStateFlow(mockMessages),
        onSendText = {},
        onStartRecord = {},
        onStopRecord = {},
        onSendVoice = {},
        onRequestAiReply = {},
        onRequestTts = {}
    )
}
