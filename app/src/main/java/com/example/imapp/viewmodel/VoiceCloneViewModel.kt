package com.example.imapp.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.imapp.repository.TtsRepository
import com.example.imapp.repository.VoiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VoiceCloneViewModel(app: Application) : AndroidViewModel(app) {
    val voices = VoiceRepository.voices
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    private val _resultPath = MutableStateFlow<String?>(null)
    val resultPath: StateFlow<String?> = _resultPath

    fun synthesize(text: String, voiceId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val file = TtsRepository.synthesize(text, voiceId, getApplication())
            _resultPath.value = file?.absolutePath
            _isLoading.value = false
        }
    }
}

