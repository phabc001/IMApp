package com.example.imapp.model

data class TtsRequest(val text: String, val voice_id: String)
data class TtsResponse(val success: Boolean, val audio_base64: String)
