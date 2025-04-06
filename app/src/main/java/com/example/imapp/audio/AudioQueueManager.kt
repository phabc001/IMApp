package com.example.imapp.audio

import android.media.MediaPlayer
import android.util.Log
import com.example.imapp.audio.model.PlaybackItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object AudioQueueManager {
    private val queue = mutableListOf<PlaybackItem>()
    private var currentIndex = 0
    private var mediaPlayer: MediaPlayer? = null

    // 用于UI层观察当前播放状态
    private val _currentPlayingState = MutableStateFlow("Idle")
    val currentPlayingState = _currentPlayingState.asStateFlow()

    private val loopItems = mutableListOf<String>() // 存放预录音频文件路径

    fun startLoop(preRecordedList: List<String>) {
        loopItems.clear()
        loopItems.addAll(preRecordedList)

        queue.clear()
        // 把预录音频先加进去
        loopItems.forEach { path ->
            queue.add(PlaybackItem(path, isLoopItem = true))
        }
        currentIndex = 0
        playCurrent()
    }

    /** 插入一段AI合成的临时音频(只播一次) */
    fun insertTtsAudio(ttsPath: String) {
        if (queue.isEmpty()) return
        val insertPos = (currentIndex + 1).coerceAtMost(queue.size)
        queue.add(insertPos, PlaybackItem(ttsPath, isLoopItem = false))
        Log.d("AudioQueueManager", "Inserted TTS audio at pos=$insertPos, path=$ttsPath")
    }

    private fun playCurrent() {
        if (currentIndex >= queue.size) {
            refillLoopItems()
            return
        }
        val item = queue[currentIndex]
        _currentPlayingState.value = "Playing: ${item.audioPath}"
        Log.d("AudioQueueManager", "playCurrent: index=$currentIndex, path=${item.audioPath}")

        // 检查文件是否存在
        val audioFile = File(item.audioPath)
        if (!audioFile.exists()) {
            Log.w("AudioQueueManager", "File not found: ${item.audioPath}, skipping this item.")
            queue.removeAt(currentIndex)
            if (queue.isEmpty()) {
                refillLoopItems()
            } else {
                playCurrent()
            }
            return
        }

        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(item.audioPath)
            prepare()
            start()
            setOnCompletionListener {
                onItemCompleted()
            }
        }
    }

    private fun onItemCompleted() {
        if (currentIndex >= queue.size) return
        val item = queue[currentIndex]
        if (!item.isLoopItem) {
            // TTS 播放完就移除
            queue.removeAt(currentIndex)
        } else {
            // 预录音频，播完就下一个
            currentIndex++
        }
        if (queue.isEmpty() || currentIndex >= queue.size) {
            refillLoopItems()
        } else {
            playCurrent()
        }
    }

    /**
     * 重新填充队列：在队列播完后，将预录音频重新加入队列。
     * 在此之前，先检查 loopItems 中是否至少有一个有效的文件存在，
     * 如果都不存在，则更新状态，不再调用 playCurrent()，以防无限循环。
     */
    private fun refillLoopItems() {
        if (loopItems.isEmpty()) {
            _currentPlayingState.value = "No audio available"
            return
        }
        // 检查 loopItems 中是否至少有一个文件存在
        val anyValid = loopItems.any { path ->
            File(path).exists()
        }
        if (!anyValid) {
            _currentPlayingState.value = "No valid audio found in loop items"
            return
        }
        // 重新填充队列并重置 currentIndex
        queue.clear()
        loopItems.forEach { path ->
            queue.add(PlaybackItem(path, isLoopItem = true))
        }
        currentIndex = 0
        playCurrent()
    }
}
