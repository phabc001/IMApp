package com.example.imapp.repository


import android.content.Context
import com.example.imapp.data.Voice
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*

object VoiceRepository {
    private val defaultVoices = listOf(
        Voice("xiaoyun", "默认女声", true),
        Voice("xiaomo", "默认男声", true)
    )
    private val _voices = MutableStateFlow(defaultVoices)
    val voices = _voices.asStateFlow()

    fun addTop(voice: Voice) {
        _voices.value = listOf(voice) + _voices.value
    }

    fun delete(id: String) {
        _voices.value = _voices.value.filterNot { !it.isDefault && it.voiceId == id }
    }

    /** 录音或上传完成后 -> OSS -> 调用创建接口 -> 回调新增 */
    suspend fun createVoice(filePath: String) {
        // TODO 上传 ORC & 调用接口，假设返回 voiceId
        val newId = "voice_" + UUID.randomUUID().toString().take(6)
        addTop(Voice(newId, "自定义音色$newId"))
    }
}
