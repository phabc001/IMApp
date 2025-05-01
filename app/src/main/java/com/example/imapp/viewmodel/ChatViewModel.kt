// ChatViewModel.kt
package com.example.imapp.viewmodel

import android.app.Application
import android.media.MediaRecorder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.imapp.network.Message
import com.example.imapp.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

class ChatViewModel(application: Application) : AndroidViewModel(application) {
    // 从 Repository 获取消息流供 UI 层收集
    val messageFlow: StateFlow<List<Message>> = ChatRepository.messageFlow

    // 1. 发送文本消息
    fun sendText(inputText: String) {
        ChatRepository.sendTextMessage(inputText)
    }

    // 2. 开始录音：初始化 MediaRecorder 并开始录音
    fun startRecording() {
        // 获取应用专用缓存目录文件路径，例如 "recorded_audio.webm"
        val cacheFile = File(getApplication<Application>().cacheDir, "recorded_audio.webm")
        try {
            // 配置并启动录音
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)  // 使用 MPEG_4 格式（扩展名 .mp4 或 .m4a，可调整）
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setOutputFile(cacheFile.absolutePath)
                prepare()
                start()
            }
            recordingFile = cacheFile
        } catch (e: Exception) {
            Log.e("ChatViewModel", "录音启动失败: ${e.message}")
        }
    }

    // 3. 停止录音
    fun stopRecording() {
        try {
            mediaRecorder?.apply {
                stop()
                reset()
                release()
            }
            mediaRecorder = null
            // 录音停止后，录音文件已保存到 recordingFile
        } catch (e: Exception) {
            Log.e("ChatViewModel", "录音停止异常: ${e.message}")
        }
    }

    // 4. 发送录制的语音消息
    fun sendVoice() {
        // 确保录音文件存在，然后通过 Repository 发送音频消息
        recordingFile?.let { file ->
            ChatRepository.sendAudioMessage(file)
        }
        // 发送完后清除临时文件引用
        recordingFile = null
    }

    // 5. 请求 AI 对话回复
    fun requestAiReply(question: String) {
        viewModelScope.launch(Dispatchers.IO) {
            ChatRepository.requestAiReply(question)
        }
    }

    // 6. 请求 AI 语音朗读
    fun requestTts(text: String) {
        viewModelScope.launch(Dispatchers.IO) {
            ChatRepository.requestTtsAudio(getApplication(), text)
        }
    }

    // 成员变量：MediaRecorder和录音文件，用于音频录制管理
    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
}
