package com.example.imapp.data

data class AudioItem(
    val id: String,                // UUID
    val name: String,              // 文件名
    val uri: String,               // File path or content Uri
    var selected: Boolean = true   // 复选播放
)