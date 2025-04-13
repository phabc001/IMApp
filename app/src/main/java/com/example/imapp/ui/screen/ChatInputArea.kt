package com.example.imapp.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Keyboard
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp

@Composable
fun ChatInputArea(
    onSendText: (String) -> Unit,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onSendVoice: () -> Unit
) {
    var isVoiceMode by remember { mutableStateOf(false) }
    var textInput by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 切换按钮：左侧图标，根据模式显示麦克风或键盘图标
        IconButton(onClick = { isVoiceMode = !isVoiceMode }) {
            Icon(
                imageVector = if (isVoiceMode) Icons.Default.Keyboard else Icons.Default.Mic,
                contentDescription = "切换输入模式"
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        if (isVoiceMode) {
            // 语音模式：显示按住说话区域，并利用 pointerInput 实现长按触发录音开始，释放触发录音结束
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .background(Color(0xFFF0F0F0), shape = MaterialTheme.shapes.small)
                    .pointerInput(Unit){
                        detectTapGestures(
                            onPress = { offset ->
                                onStartRecord()
                                tryAwaitRelease()  // 等待手指释放后
                                onStopRecord()
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "按住 说话", color = Color.Gray)
            }
        } else {
            // 文字模式：显示文本输入框
            TextField(
                value = textInput,
                onValueChange = { textInput = it },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                placeholder = { Text("输入消息") },
                singleLine = true
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        if (isVoiceMode) {
            // 语音模式下：显示发送语音按钮
            Button(
                onClick = { onSendVoice() },
                modifier = Modifier.height(48.dp)
            ) {
                Text("发送语音")
            }
        } else {
            // 文字模式下：显示发送文本按钮
            Button(
                onClick = {
                    onSendText(textInput)
                    textInput = ""
                },
                modifier = Modifier.height(48.dp)
            ) {
                Text("发送")
            }
        }
    }
}
