package com.example.imapp.viewmodel


import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.imapp.audio.AudioQueueManager
import com.example.imapp.data.AudioItem
import com.example.imapp.repository.AudioRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AudioManagerViewModel(app: Application) : AndroidViewModel(app) {
    val audioList: StateFlow<List<AudioItem>> = AudioRepository.audioList
    val playingItem = AudioQueueManager.playingItem

    fun loadAudios() {
        AudioRepository.loadFromFolder(getApplication())
    }

    fun reorder(from: Int, to: Int) = AudioRepository.reorder(from, to)
    fun toggleSelect(id: String) = AudioRepository.toggleSelect(id)

    fun play(loop: Boolean) {
        AudioQueueManager.init(getApplication())
        AudioQueueManager.playQueue(audioList.value, loop)
    }

    fun pause() = AudioQueueManager.pause()
    fun resume() = AudioQueueManager.resume()
}
