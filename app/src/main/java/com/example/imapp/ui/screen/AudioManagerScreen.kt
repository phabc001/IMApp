package com.example.imapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun AudioManagerScreen(modifier: Modifier = Modifier) {
    // 这里维护一个预录音频列表
    // 正式项目中可以通过ViewModel/Repository拿到数据
    val audioList = remember {
        mutableStateListOf<String>()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text("预录音频管理", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(16.dp))

        if(audioList.isEmpty()){
            Text("目前没有预录音频文件", color=Color.Red)

        }
        else {

            // 简单列举
            audioList.forEachIndexed { index, audioName ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("$index. $audioName")
                    Button(onClick = {
                        // TODO: 预览播放 or remove
                    }) {
                        Text("播放预览")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(onClick = {
            // TODO: 打开对话框输入文本 -> TTS -> 生成新的音频 -> 加入audioList
        }) {
            Text("添加新的预录音频")
        }
    }
}

/** 预览函数：显示 AudioManagerScreen 布局 */
@Preview(showBackground = true, name = "AudioManagerScreenPreview")
@Composable
fun AudioManagerScreenPreview() {
    AudioManagerScreen()
}
