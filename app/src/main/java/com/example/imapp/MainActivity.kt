package com.example.imapp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.*
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import com.example.imapp.network.Message
import com.example.imapp.ui.theme.IMAppTheme
import com.example.imapp.viewmodel.ChatViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.imapp.service.ChatService


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 启动前台聊天服务
        val serviceIntent = Intent(this, ChatService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)

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

        // 设置 Compose 内容
        setContent {
            IMAppTheme {
                // 获取 ViewModel（需要在 Activity 范围内）
                val viewModel: ChatViewModel = viewModel()
                HomeScreen(
                    messageFlow = viewModel.messageFlow as MutableStateFlow<List<Message>>,
                    onSendText = { text -> viewModel.sendText(text) },
                    onStartRecord = { viewModel.startRecording() },
                    onStopRecord = { viewModel.stopRecording() },
                    onSendVoice = { viewModel.sendVoice() },
                    onRequestAiReply = { question -> viewModel.requestAiReply(question) },
                    onRequestTts = { text -> viewModel.requestTts(text) }
                )
            }
        }
    }
}
