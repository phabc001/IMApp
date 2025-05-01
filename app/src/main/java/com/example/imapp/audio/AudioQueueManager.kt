package com.example.imapp.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.AudioAttributes
import androidx.media3.exoplayer.ExoPlayer
import com.example.imapp.data.AudioItem
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File

object AudioQueueManager {
    private var mainPlayer: ExoPlayer? = null          // 主队列播放器
    private var tempPlayer: ExoPlayer? = null          // 临时插播播放器
    private var loopFlag   = false

    // 新增：记录所有临时插入的 TTS ID
    private val tempMap = mutableMapOf<String, String>()  // id → file path


    private val _playingItem = MutableStateFlow<AudioItem?>(null)
    val playingItem = _playingItem.asStateFlow()

    /* 当前播放列表（已过滤未勾选）*/
    private var currentList: List<AudioItem> = emptyList()

    /* 恢复主播放器的位置 */
    private var resumePosition: Long = 0L

    fun init(ctx: Context) {
        if (mainPlayer != null) return
        val appCtx = ctx.applicationContext
        mainPlayer = ExoPlayer.Builder(appCtx).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                true
            )
            addListener(object : Player.Listener {
                private var previousMediaId: String? = null
                override fun onMediaItemTransition(item: MediaItem?, reason: Int) {

                    // 清理上一首临时 TTS
                    previousMediaId?.takeIf { it in tempMap }?.let { tempId ->
                        val path = tempMap.remove(tempId)
                        removeFromQueue(tempId)
                        path?.let { File(it).takeIf(File::exists)?.delete() }
                    }

                    previousMediaId = item?.mediaId
                    _playingItem.value = previousMediaId
                        ?.let { id -> currentList.find { it.id == id } }

                }

            })
        }
    }

    /* ---------------- 队列播放 ---------------- */
    fun playQueue(list: List<AudioItem>, loop: Boolean) {
        currentList = list.filter { it.selected }
        if (currentList.isEmpty()) return
        loopFlag = loop
        mainPlayer?.apply {
            repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            setMediaItems(currentList.map { it.toMediaItem() }, true)
            prepare()
            playWhenReady = true
        }
    }

    /** 勾选状态改变后调用，刷新播放列表 */
    fun refreshQueue() {
        if (!isPlaying) return
        playQueue(currentList, loopFlag)
    }


    /* ---------------- 控制 ---------------- */
    val isPlaying: Boolean get() = mainPlayer?.isPlaying == true
    fun pause() = mainPlayer?.pause()
    fun resume() = mainPlayer?.play()

    fun release() {
        mainPlayer?.release(); mainPlayer = null
        tempPlayer?.release(); tempPlayer = null
    }

    /* 扩展：AudioItem -> MediaItem */
    private fun AudioItem.toMediaItem() =
        MediaItem.Builder().setUri(Uri.parse(uri)).setMediaId(id).setTag(name).build()

    /**
     * 将 items 插入到当前播放项之后
     */
    fun insertTempAudioAfter(items: List<AudioItem>) {
        mainPlayer?.let { player ->
            // 找到当前播放索引
            val idx = player.currentMediaItemIndex
            // 构建 MediaItems
            val mediaItems = items.map { it.toMediaItem() }
            // 在 index+1 位置插入
            player.addMediaItems(idx + 1, mediaItems)

            // 同步更新内部 currentList
            val before = currentList.take(idx + 1)
            val after  = currentList.drop(idx + 1)
            currentList = before + items + after

            // 标记为临时，播完后自动清理
            items.forEach { tempMap[it.id] = it.uri }

        }
    }


    fun removeFromQueue(id: String) {
        val player = mainPlayer ?: return

        // 1. 找到要删除的索引
        val count = player.mediaItemCount
        val idx = (0 until count).indexOfFirst { i ->
            player.getMediaItemAt(i).mediaId == id
        }
        if (idx < 0) return

        // 2. 判断是不是当前正在播放的那首
        val isCurrent = idx == player.currentMediaItemIndex

        // 3. 从 ExoPlayer 的队列中移除
        player.removeMediaItem(idx)

        // 4. 同步更新内部 currentList
        currentList = currentList.filterNot { it.id == id }

        // 5. 如果删的是“当前”那首，切到下一首或暂停
        if (isCurrent) {
            val newCount = player.mediaItemCount
            if (newCount > idx) {
                // 播放新索引处这首歌
                player.seekTo(idx, /* positionMs = */ 0L)
                player.playWhenReady = true
            } else {
                // 没有下一首，停播
                player.pause()
                _playingItem.value = null
            }
        }
    }
}