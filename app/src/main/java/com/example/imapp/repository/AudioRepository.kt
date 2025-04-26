package com.example.imapp.repository


import android.content.Context
import com.example.imapp.data.AudioItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.util.*

object AudioRepository {
    private val _audioList = MutableStateFlow<List<AudioItem>>(emptyList())
    val audioList = _audioList.asStateFlow()

    /** 初次加载本地目录音频 */
    fun loadFromFolder(ctx: Context) {
        val dir = File(ctx.filesDir, "audios")
        if (!dir.exists()) dir.mkdirs()
        val list = dir.listFiles()?.map {
            AudioItem(UUID.randomUUID().toString(), it.name, it.absolutePath)
        } ?: emptyList()
        _audioList.value = list
    }

    /** 新增音频（克隆完成或上传文件后调用） */
    fun addAudio(file: File) {
        val item = AudioItem(UUID.randomUUID().toString(), file.name, file.absolutePath)
        _audioList.value = _audioList.value + item
    }

    fun addAudios(files: List<File>) {
        val newItems = files.map { AudioItem(UUID.randomUUID().toString(), it.name, it.absolutePath) }
        _audioList.value = _audioList.value + newItems
    }

    fun deleteAudio(id: String) {
        _audioList.value = _audioList.value.filterNot { it.id == id }
    }

    /** 拖动排序 */
    fun reorder(from: Int, to: Int) {
        val mutable = _audioList.value.toMutableList()
        val item = mutable.removeAt(from)
        mutable.add(to, item)
        _audioList.value = mutable
    }

    /** 复选切换 */
    fun toggleSelect(id: String) {
        _audioList.value = _audioList.value.map { if (it.id == id) it.copy(selected = !it.selected) else it }
    }
}
