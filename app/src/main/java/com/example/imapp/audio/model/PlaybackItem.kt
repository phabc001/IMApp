package com.example.imapp.audio.model

data class PlaybackItem(
    val audioPath: String,
    val isLoopItem: Boolean // true=预录音频(循环), false=一次性TTS
)
