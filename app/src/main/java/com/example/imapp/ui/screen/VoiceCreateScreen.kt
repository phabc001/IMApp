package com.example.imapp.ui.screen

import android.Manifest
import android.media.MediaRecorder
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.example.imapp.viewmodel.UploadState
import com.example.imapp.viewmodel.VoiceCreateViewModel
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCreateScreen(
    nav: NavController,
    vm: VoiceCreateViewModel = viewModel()
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    /* ---------- 录音相关状态 ---------- */
    var isRecording by remember { mutableStateOf(false) }
    var recorder   by remember { mutableStateOf<MediaRecorder?>(null) }
    var pickedFile by remember { mutableStateOf<File?>(null) }

    /* ---------- 预览播放器状态 ---------- */
    val previewPlayerState = remember { mutableStateOf<ExoPlayer?>(null) }

    /* ---------- 预览函数 ---------- */
    fun previewFile(file: File) {
        // 释放老播放器
        previewPlayerState.value?.release()
        // 新播放器
        val player = ExoPlayer.Builder(ctx).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            prepare()
            play()
        }
        previewPlayerState.value = player
    }

    /* ---------- 录音逻辑 ---------- */
    fun startRecord() {
        val file = File(ctx.cacheDir, "voice_${UUID.randomUUID()}.m4a")
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            prepare()
            start()
        }
        pickedFile = file
        isRecording = true
        // 停用上次的预览
        previewPlayerState.value?.release()
        previewPlayerState.value = null
    }

    fun stopRecord() {
        try {
            recorder?.apply {
                try { stop() } catch (_: RuntimeException) { /* 删除坏文件 */ }
                release()
            }
        } catch (_: Exception) { /* 忽略 */ }
        recorder = null
        isRecording = false

        // 录制完后自动预览
        pickedFile?.let { file -> previewFile(file) }
    }



    /* ---------- 权限 & 文件选择 ---------- */
    val permLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted) startRecord()
        else Toast.makeText(ctx, "录音权限被拒绝", Toast.LENGTH_SHORT).show()
    }
    val filePicker = rememberLauncherForActivityResult(OpenDocument()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        // 简单过滤 MIME
        val type = ctx.contentResolver.getType(uri) ?: ""
        if (!type.startsWith("audio/")) {
            Toast.makeText(ctx, "请选择音频文件", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        val file = copyUriToCache(ctx, uri) ?: return@rememberLauncherForActivityResult
        pickedFile = file
        previewFile(file)
    }

    /* ---------- 上传状态 ---------- */
    val uploadState by vm.state.collectAsState()

    /* ---------- UI ---------- */
    Scaffold(topBar = { TopAppBar(title = { Text("创建音色") }) }) { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 录音按钮
            OutlinedButton(
                onClick = {
                    if (isRecording) stopRecord()
                    else permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isRecording) "停止录音" else "开始录音")
            }

            // 文件选择按钮
            OutlinedButton(
                onClick = { filePicker.launch(arrayOf("audio/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AudioFile, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("从文件选择音频")
            }

            // 预览提示
            if (pickedFile != null) {
                AssistChip(
                    onClick = { previewFile(pickedFile!!) },
                    label = { Text("已选音频：${pickedFile!!.name}") },
                    leadingIcon = { Icon(Icons.Default.Check, contentDescription = null) }
                )
            }

            // 上传并创建
            Button(
                enabled = pickedFile != null && uploadState is UploadState.Idle,
                onClick = {
                    pickedFile?.let {
                        val name = it.nameWithoutExtension.take(12)
                        vm.reset()
                        vm.uploadAndCreate(it, name)
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("上传并创建")
            }

            // 状态展示
            when (uploadState) {
                UploadState.Uploading -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator()
                        Spacer(Modifier.width(8.dp))
                        Text("上传 / 创建中…")
                    }
                }
                is UploadState.Success -> {
                    Text("创建成功：ID = ${(uploadState as UploadState.Success).voiceId}")
                    LaunchedEffect(Unit) { nav.popBackStack() }
                }
                is UploadState.Error -> {
                    Text("创建失败：${(uploadState as UploadState.Error).msg}")
                }
                else -> {}
            }
        }
    }

    /* ---------- 退出释放播放器 ---------- */
    DisposableEffect(Unit) {
        onDispose { previewPlayerState.value?.release() }
    }
}

/** 工具：将 SAF Uri 复制到应用缓存目录 */
private fun copyUriToCache(ctx: android.content.Context, uri: Uri): File? = try {
    val name = ctx.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        cursor.moveToFirst()
        cursor.getString(idx)
    } ?: "audio_${UUID.randomUUID()}.tmp"
    val dest = File(ctx.cacheDir, name)
    ctx.contentResolver.openInputStream(uri)?.use { input ->
        dest.outputStream().use { input.copyTo(it) }
    }
    dest
} catch (e: Exception) {
    e.printStackTrace()
    null
}
