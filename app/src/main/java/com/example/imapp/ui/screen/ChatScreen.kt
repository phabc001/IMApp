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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.imapp.audio.AudioPlayerUtil
import com.example.imapp.network.Message
import com.example.imapp.viewmodel.ChatViewModel


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChatScreen(
    modifier: Modifier = Modifier,
    viewModel: ChatViewModel = viewModel()

) {
    // 只做展示：直接从 VM 拿流
    val messages by viewModel.messageFlow.collectAsState()

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
                                                            viewModel.requestTts(msg.text) // 调用外部 TTS 回调
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
                                                        viewModel.requestAiReply(msg.text)
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
            onSendText = viewModel::sendText,
            onStartRecord = viewModel::startRecording,
            onStopRecord = viewModel::stopRecording,
            onSendVoice = viewModel::sendVoice
        )
    }
}
