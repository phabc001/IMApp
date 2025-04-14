package com.example.imapp.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import java.io.File
import java.io.FileOutputStream

object AudioPlayerUtil {
    private var mediaPlayer: MediaPlayer? = null

    fun playBase64Audio(context: Context, base64Audio: String) {
        try {
            // 1. 解码 Base64 成字节
            val audioBytes = Base64.decode(base64Audio, Base64.DEFAULT)

            // 2. 写入临时 mp3 文件
            val tempFile = File.createTempFile("tts_", ".mp3", context.cacheDir)
            val fos = FileOutputStream(tempFile)
            fos.write(audioBytes)
            fos.close()

            // 3. 释放之前的播放器
            mediaPlayer?.release()

            // 4. 创建并播放
            mediaPlayer = MediaPlayer().apply {
                setDataSource(tempFile.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    it.release()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
