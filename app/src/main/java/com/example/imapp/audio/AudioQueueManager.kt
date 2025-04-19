package com.example.imapp.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.AudioAttributes
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.exoplayer.ExoPlayer
import com.example.imapp.data.AudioItem
import com.example.imapp.repository.AudioRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * 单例队列管理，支持循环播放、顺序播放和临时插播
 */
object AudioQueueManager {
    private var player: ExoPlayer? = null
    private var tempPlayer: ExoPlayer? = null

    private val _playingItem = MutableStateFlow<AudioItem?>(null)
    val playingItem = _playingItem.asStateFlow()

    private val mainScope: CoroutineScope = MainScope()

    /**
     * 初始化播放器，务必传入 Application Context
     */
    fun init(context: Context) {
        if (player == null) {
            val appCtx = context.applicationContext

            // 主播放器
            player = ExoPlayer.Builder(appCtx)
                .build()
                .apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(C.USAGE_MEDIA)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .build(),
                        true
                    )
                    repeatMode = ExoPlayer.REPEAT_MODE_OFF
                    addListener(object : Player.Listener {
                        override fun onMediaItemTransition(item: MediaItem?, reason: Int) {
                            item?.mediaId?.let { id ->
                                mainScope.launch(Dispatchers.Main) {
                                    _playingItem.value =
                                        AudioRepository.audioList.value.find { it.id == id }
                                }
                            }
                        }
                    })
                }

            // 临时播放器，用于插播
            tempPlayer = ExoPlayer.Builder(appCtx)
                .build()
                .apply {
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(C.USAGE_MEDIA)
                            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                            .build(),
                        true
                    )
                }
        }
    }

    /**
     * 设置并播放队列（顺序或循环）
     */
    fun playQueue(list: List<AudioItem>, loop: Boolean) {
        player?.let { exo ->
            val items = list.filter { it.selected }.map { item ->
                MediaItem.Builder()
                    .setUri(Uri.parse(item.uri))
                    .setMediaId(item.id)
                    .setTag(item.name)
                    .build()
            }
            exo.apply {
                repeatMode = if (loop) REPEAT_MODE_ALL else REPEAT_MODE_OFF
                setMediaItems(items, /* resetPosition= */ true)
                prepare()
                playWhenReady = true
            }
        }
    }

    fun pause() {
        player?.pause()
    }

    fun resume() {
        player?.play()
    }

    /**
     * 插播：暂停当前队列，临时播放器播放，结束后继续主播放器
     */
    fun insertAndPlay(audio: AudioItem) {
        player?.let { mainPlayer ->
            val resumePosition = mainPlayer.currentPosition
            mainPlayer.pause()

            tempPlayer?.apply {
                // 清理旧数据
                stop()
                clearMediaItems()

                // 设置并播放插播音频
                setMediaItem(
                    MediaItem.Builder()
                        .setUri(Uri.parse(audio.uri))
                        .setMediaId(audio.id)
                        .build()
                )
                prepare()
                playWhenReady = true

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        if (state == Player.STATE_ENDED) {
                            // 停止临时播放器，并继续主播放器
                            clearMediaItems()
                            mainPlayer.seekTo(resumePosition)
                            mainPlayer.play()
                        }
                    }
                })
            }
        }
    }

    /**
     * 释放所有播放器资源，调用于 App 退出或不再需要时
     */
    fun release() {
        player?.release()
        player = null
        tempPlayer?.release()
        tempPlayer = null
    }
}
