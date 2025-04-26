package com.example.imapp.viewmodel


import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.imapp.audio.AudioQueueManager
import com.example.imapp.data.AudioItem
import com.example.imapp.repository.AudioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class AudioManagerViewModel(app: Application) : AndroidViewModel(app) {
    val audioList: StateFlow<List<AudioItem>> = AudioRepository.audioList
    val playingItem = AudioQueueManager.playingItem

    init { AudioRepository.loadFromFolder(getApplication()) }

    suspend fun importList(ctx: Context, uris: List<Uri>) = withContext(Dispatchers.IO) {
        val dir = File(ctx.filesDir, "audios").apply { mkdirs() }
        val newFiles = mutableListOf<File>()
        for (uri in uris) {
            val name = queryDisplayName(ctx, uri) ?: "import_${System.currentTimeMillis()}.wav"
            val dest = ensureUniqueFile(dir, name)
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                dest.outputStream().use { output -> input.copyTo(output) }
            }
            newFiles.add(dest)
        }
        AudioRepository.addAudios(newFiles)
    }

    private fun ensureUniqueFile(dir: File, baseName: String): File {
        var file = File(dir, baseName)
        var idx = 1
        val ext = file.extension
        val pure = file.nameWithoutExtension
        while (file.exists()) {
            file = File(dir, "${pure}(${idx++}).$ext")
        }
        return file
    }

    private fun queryDisplayName(ctx: Context, uri: Uri): String? =
        ctx.contentResolver.query(uri, null, null, null, null)
            ?.use { c ->
                val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
            }

    fun delete(id: String) {
        val removedPlaying = playingItem.value?.id == id
        AudioRepository.deleteAudio(id)
        if (removedPlaying) AudioQueueManager.refreshQueue()
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
