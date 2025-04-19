package com.example.imapp.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.imapp.repository.VoiceRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class VoiceManagementViewModel : ViewModel() {
    val voices = VoiceRepository.voices

    fun delete(id: String) = VoiceRepository.delete(id)

    fun create(path: String) = viewModelScope.launch {
        VoiceRepository.createVoice(path)
    }
}
