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
import kotlinx.coroutines.launch

object AudioQueueManager {
    private var mainPlayer: ExoPlayer? = null          // 主队列播放器
    private var tempPlayer: ExoPlayer? = null          // 临时插播播放器
    private var loopFlag   = false

    private val _playingItem = MutableStateFlow<AudioItem?>(null)
    val playingItem = _playingItem.asStateFlow()

    /* 当前播放列表（已过滤未勾选）*/
    private var currentList: List<AudioItem> = emptyList()

    /* 恢复主播放器的位置 */
    private var resumePosition: Long = 0L

    private val scope = MainScope()

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
                override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                    _playingItem.value = item?.mediaId?.let { id ->
                        currentList.find { it.id == id }
                    }
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

    /* ---------------- 临时插播 ---------------- */
    fun playInterlude(ctx: Context, audio: AudioItem) {
        val main = mainPlayer ?: return
        resumePosition = main.currentPosition
        main.pause()

        val appCtx = ctx.applicationContext
        if (tempPlayer == null) {
            tempPlayer = ExoPlayer.Builder(appCtx).build().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(C.USAGE_MEDIA)
                        .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
                        .build(),
                    true
                )
            }
        }
        tempPlayer?.apply {
            clearMediaItems()
            setMediaItem(audio.toMediaItem())
            prepare()
            playWhenReady = true
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        // 插播结束，释放 temp 并恢复主播放器
                        clearMediaItems()
                        playWhenReady = false
                        main.seekTo(resumePosition)
                        main.play()
                    }
                }
            })
        }
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
}