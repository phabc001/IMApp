// AudioQueueManager.kt
package com.example.imapp.audio

import android.content.Context
import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.AudioAttributes
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import com.example.imapp.data.AudioItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.io.File
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 主播放队列管理，支持互斥的临时 TTS 播放
 */
object AudioQueueManager {
    private var mainPlayer: ExoPlayer? = null
    private var tempPlayer: ExoPlayer? = null
    private lateinit var appContext: Context

    // 主播放队列
    private val queue = mutableListOf<AudioItem>()
    private var currentIndex = 0

    // 待插播的 TTS 列表
    private val pendingTemp = CopyOnWriteArrayList<AudioItem>()
    private var isTempPlaying = false


    private val _playingItem = MutableStateFlow<AudioItem?>(null)
    val playingItem: StateFlow<AudioItem?> = _playingItem


    private val _isMainPlaying = MutableStateFlow(false)
    val isMainPlaying: StateFlow<Boolean> = _isMainPlaying

    private var wasMainPlayingBeforeTemp: Boolean = false


    /**
     * 初始化播放器，需要在 App 启动或首次调用时执行
     */
    @OptIn(UnstableApi::class)
    fun init(context: Context) {
        appContext = context.applicationContext
        if (mainPlayer != null) return

        mainPlayer = ExoPlayer.Builder(appContext).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(), true
            )
            addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _isMainPlaying.value = isPlaying

                }
                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    // 主播放器切到下一首前触发
                    if (pendingTemp.isNotEmpty() && !isTempPlaying) {
                        pause()
                        playTempList()
                    } else {
                        currentIndex = this@apply.currentMediaItemIndex
                        val current = queue.getOrNull(currentIndex)
                        _playingItem.value = current
                    }


                }
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        // 重置队列和状态
                        _playingItem.value = null
                        _isMainPlaying.value = false
                    }
                }
            })
        }
    }

    /**
     * 播放主列表，可指定循环
     */
    fun playQueue(list: List<AudioItem>, loop: Boolean) {
        // 重置临时播放状态
        pendingTemp.clear()
        isTempPlaying = false
        releaseTempPlayer()

        // 更新队列
        queue.clear()
        queue.addAll(list)
        currentIndex = 0

        mainPlayer?.apply {
            repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
            setMediaItems(queue.map { it.toMediaItem() }, /* resetPosition= */ true)
            prepare()
            playWhenReady = true
        }
    }

    fun pause() {
        if (isTempPlaying) tempPlayer?.pause() else mainPlayer?.pause()
    }
    fun resume() {
        if (isTempPlaying) tempPlayer?.play() else mainPlayer?.play()
    }
    fun stop() {
        if (isTempPlaying) tempPlayer?.stop() else mainPlayer?.stop()
    }

    fun release() {
        queue.clear()
        pendingTemp.clear()
        mainPlayer?.release(); mainPlayer = null
        releaseTempPlayer()
    }

    @OptIn(UnstableApi::class)
    fun playTts(context: Context, items: List<AudioItem>) {
        init(context)
        if (items.isEmpty()) {
            Log.w("AudioQueueManager", "TTS 插播请求为空，忽略播放。")
            return
        }

        val isMainPlayerPlaying = mainPlayer?.isPlaying == true
        val hasMediaItems = (mainPlayer?.mediaItemCount ?: 0) > 0
        val isMainPlayerActive = isMainPlayerPlaying && hasMediaItems

        Log.i("AudioQueueManager", "TTS 插播触发：isPlaying=$isMainPlayerPlaying, hasMediaItems=$hasMediaItems")

        wasMainPlayingBeforeTemp = isMainPlayerPlaying

        if (isMainPlayerActive) {
            Log.i("AudioQueueManager", "主播放器正在播放，TTS 插播加入待播放队列，等待当前播放完成。")
            pendingTemp.clear()
            pendingTemp.addAll(items)
            return
        }

        // 主播放器空闲，立即播放 TTS
        Log.i("AudioQueueManager", "主播放器空闲，立即播放 TTS 插播。")
        pendingTemp.clear()
        pendingTemp.addAll(items)
        playTempList()
    }

    /**
     * 播放临时列表
     */
    @OptIn(UnstableApi::class)
    private fun playTempList() {
        if (pendingTemp.isEmpty()) return
        isTempPlaying = true

        // 停止并释放旧 tempPlayer
        releaseTempPlayer()
        tempPlayer = ExoPlayer.Builder(appContext).build().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(), true
            )
            setMediaItems(pendingTemp.map { it.toMediaItem() }, /* resetPosition= */ true)
            prepare()
            playWhenReady = true
            addListener(object : Player.Listener {

                override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                    // 1. 拿到 tempPlayer 当前的索引
                    val idx = tempPlayer?.currentMediaItemIndex ?: return
                    // 2. 取出 pendingTemp 列表里对应的 AudioItem
                    _playingItem.value = pendingTemp.getOrNull(idx)
                }

                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        // 清理临时文件
                        pendingTemp.forEach { File(it.uri).delete() }
                        pendingTemp.clear()
                        releaseTempPlayer()
                        isTempPlaying = false
                        // 恢复主播放器
                        // 恢复主播放器：同样用 mainPlayer.currentMediaItemIndex 拿索引
                        val mainIdx = mainPlayer?.currentMediaItemIndex ?: return
                        _playingItem.value = queue.getOrNull(mainIdx)

                        if (wasMainPlayingBeforeTemp) {
                            mainPlayer?.play()
                        }
                    }
                }
            })
        }
    }

    private fun releaseTempPlayer() {
        tempPlayer?.release()
        tempPlayer = null
    }

    private fun AudioItem.toMediaItem(): MediaItem =
        MediaItem.Builder()
            .setUri(Uri.parse(uri))
            .setMediaId(id)
            .setTag(name)
            .build()


    fun setLoop(loop: Boolean) {
        mainPlayer?.repeatMode = if (loop) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
    }


}
