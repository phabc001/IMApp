package com.example.imapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * 创建新音色流程：录音 或 上传文件 → OSS → 调用创建接口
 * 这里只给占位界面，便于后续填充
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCreateScreen() {
    Scaffold(topBar = { TopAppBar(title = { Text("创建音色") }) }) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(onClick = { /* TODO 录音 */ }) { Text("录音") }
            Button(onClick = { /* TODO 选择文件 */ }) { Text("上传音频") }
            Spacer(Modifier.height(24.dp))
            Button(onClick = { /* TODO 开始创建 */ }) { Text("提交") }
        }
    }
}
