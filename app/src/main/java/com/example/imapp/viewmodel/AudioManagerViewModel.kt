package com.example.imapp.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.example.imapp.audio.AudioQueueManager
import com.example.imapp.data.AudioItem
import com.example.imapp.repository.AudioRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private const val TAG = "AudioVM"

class AudioManagerViewModel(app: Application) : AndroidViewModel(app) {
    val audioList: StateFlow<List<AudioItem>> = AudioRepository.audioList
    val playingItem = AudioQueueManager.playingItem

    init { AudioRepository.loadFromFolder(getApplication()) }

    suspend fun importList(ctx: Context, uris: List<Uri>): Pair<Int, Int> =
        withContext(Dispatchers.IO) {
            if (uris.isEmpty()) 0 to 0 else runImport(ctx, uris)
        }

    private suspend fun runImport(ctx: Context, uris: List<Uri>): Pair<Int, Int> {
        val dir = File(ctx.filesDir, "audios").apply { mkdirs() }

        val existedUris = audioList.value.map { it.uri }.toHashSet()
        val existedNames = dir.list()?.toHashSet() ?: hashSetOf()

        var added = 0
        var skipped = 0
        val newFiles = mutableListOf<File>()

        for (raw in uris) {
            // ---------- 1. 仅对 Tree Uri 做归一化 ----------
            val uri = if (DocumentsContract.isTreeUri(raw)) {
                try {
                    ctx.contentResolver.takePersistableUriPermission(
                        raw,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    DocumentsContract.buildDocumentUriUsingTree(
                        raw,
                        DocumentsContract.getDocumentId(raw)
                    )
                } catch (e: Exception) {
                    Log.w(TAG, "TreeUri build failed: ${e.message}")
                    raw
                }
            } else raw

            // ---------- 2. URI 去重 ----------
            if (!existedUris.add(uri.toString())) {
                Log.d(TAG, "DuplicateUri → skipped: $uri"); skipped++; continue
            }

            // ---------- 3. 生成文件名，必要时补扩展名 ----------
            val name = buildFilename(ctx, queryDisplayName(ctx, uri), uri)
            if (!existedNames.add(name)) {
                Log.d(TAG, "DuplicateName → skipped: $name"); skipped++; continue
            }

            // ---------- 4. 写文件 ----------
            val dest = File(dir, name)
            val copySucceeded: Boolean = try {
                ctx.contentResolver.openInputStream(uri)?.use { ins ->
                    dest.outputStream().use { outs ->
                        ins.copyTo(outs)
                    }
                    true // 成功复制
                } ?: false // 打开流失败
            } catch (e: Exception) {
                Log.e(TAG, "CopyFailed: ${e.message}"); false
            }

            if (copySucceeded) {
                newFiles.add(dest)
                added++
            } else {
                File(dir, name).takeIf { it.exists() }?.delete()
                existedUris.remove(uri.toString())
                existedNames.remove(name)
                skipped++
            }
        }

        if (newFiles.isNotEmpty()){
            withContext(Dispatchers.Main) {
                AudioRepository.addAudios(newFiles)

                // 获取当前播放项 ID
                val currentId = playingItem.value?.id

                // 获取新增对应的 AudioItem 列表
                val allItems = audioList.value
                val newItems = newFiles.mapNotNull { file ->
                    allItems.find { it.name == file.name }
                }
                if (currentId != null && newItems.isNotEmpty()) {
                    // 假设 AudioQueueManager 提供插入接口
                    AudioQueueManager.insertTempAudioAfter(newItems)
                } else {
                    // 回退到整体刷新
                    AudioQueueManager.refreshQueue()
                }
            }

        }

        Log.i(TAG, "Import finished → added=$added, skipped=$skipped")
        return added to skipped
    }

    /**
     * 根据 DISPLAY_NAME 和 mimeType 构造最终文件名。
     */
    private fun buildFilename(ctx: Context, display: String?, uri: Uri): String {
        var base = display ?: "import_${System.currentTimeMillis()}"
        if (base.contains('.')) return base
        val mime = ctx.contentResolver.getType(uri) ?: "audio/mpeg"
        val ext = android.webkit.MimeTypeMap.getSingleton()
            .getExtensionFromMimeType(mime.lowercase(Locale.ROOT)) ?: "mp3"
        return "$base.$ext"
    }

    private fun queryDisplayName(ctx: Context, uri: Uri): String? =
        ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val idx = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && c.moveToFirst()) c.getString(idx) else null
        }


    fun delete(id: String) {
        // 1. 先找到要删的 AudioItem
        val current = audioList.value.firstOrNull { it.id == id }

        // 2. 物理删掉私有目录里的文件
        current?.let { File(it.uri).takeIf { f -> f.exists() }?.delete() }

        // 3. 从播放队列移除
        AudioQueueManager.removeFromQueue(id)

        // 4. 更新内存列表
        AudioRepository.deleteAudio(id)

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
