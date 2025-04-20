package com.example.imapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.imapp.repository.VoiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.File

/* ---------- UI 状态 ---------- */
sealed interface UploadState {
    object Idle : UploadState
    object Uploading : UploadState
    data class Success(val voiceId: String) : UploadState
    data class Error(val msg: String) : UploadState
}

class VoiceCreateViewModel(app: Application) : AndroidViewModel(app) {

    /* 公开给 UI 收集的状态 */
    private val _state = MutableStateFlow<UploadState>(UploadState.Idle)
    val state: StateFlow<UploadState> = _state

    /** 重置为 Idle，供 UI 在每次进入页面时调用 */
    fun reset() { _state.value = UploadState.Idle }

    /** 开始上传并创建 voice_id */
    fun uploadAndCreate(file: File, displayName: String) {
        if (_state.value == UploadState.Uploading) return   // 避免重复提交
        _state.value = UploadState.Uploading

        viewModelScope.launch {
            val result = VoiceRepository.uploadAndCreate(file, displayName)
            _state.value = result.fold(
                onSuccess = { UploadState.Success(it.voiceId) },
                onFailure = { UploadState.Error(it.message ?: "未知错误") }
            )
        }
    }
}
