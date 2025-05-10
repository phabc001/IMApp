package com.example.imapp.ui.component


import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

@Composable
fun rememberAudioPlayer(filePath: String): () -> Unit {
    val context = LocalContext.current
    val player = remember { ExoPlayer.Builder(context).build() }

    DisposableEffect(filePath) {
        val uri = Uri.parse(filePath)
        val mediaItem = MediaItem.fromUri(uri)
        player.setMediaItem(mediaItem)
        player.prepare()
        onDispose {
            player.release()
        }
    }

    return {
        player.playWhenReady = true
    }
}
