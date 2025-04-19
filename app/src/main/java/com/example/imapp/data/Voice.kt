package com.example.imapp.data

data class Voice(
    val voiceId: String,
    val displayName: String,
    val isDefault: Boolean = false
)