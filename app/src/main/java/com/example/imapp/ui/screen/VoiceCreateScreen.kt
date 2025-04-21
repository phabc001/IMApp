package com.example.imapp.ui.screen

import android.Manifest
import android.media.MediaRecorder
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.activity.result.contract.ActivityResultContracts.RequestPermission
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.navigation.NavController
import com.example.imapp.viewmodel.UploadState
import com.example.imapp.viewmodel.VoiceCreateViewModel
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCreateScreen(
    nav: NavController,
    vm: VoiceCreateViewModel = viewModel()
) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }

    // 录音状态
    var isRecording by remember { mutableStateOf(false) }
    var recorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordStart by remember { mutableStateOf(0L) }
    var recordElapsed by remember { mutableStateOf(0L) }

    // 播放器 & 文件状态
    var pickedFile by remember { mutableStateOf<File?>(null) }
    val playerState = remember { mutableStateOf<ExoPlayer?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var durationMs by remember { mutableStateOf(0L) }
    var positionMs by remember { mutableStateOf(0L) }

    // 上传状态
    val uploadState by vm.state.collectAsState()

    // 加载音频但不播放，读取总时长 on STATE_READY
    fun loadForPreview(file: File) {
        playerState.value?.release()
        val player = ExoPlayer.Builder(ctx).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        durationMs = duration.coerceAtLeast(0L)
                    }
                    if (state == Player.STATE_ENDED) {
                        isPlaying = false
                    }
                }
            })
            setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
            prepare()
        }
        playerState.value = player
        positionMs = 0L
        isPlaying = false
    }

    // 录音开始
    fun startRecord() {
        val file = File(ctx.cacheDir, "voice_${System.currentTimeMillis()}.m4a")
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(file.absolutePath)
            prepare(); start()
        }
        recordStart = System.currentTimeMillis()
        pickedFile = file
        isRecording = true
        playerState.value?.release()
        playerState.value = null
    }

    // 录音停止
    fun stopRecord() {
        try {
            recorder?.apply { stop(); release() }
        } catch (_: Exception) {
            pickedFile?.delete()
            Toast.makeText(ctx, "录音失败，请重试", Toast.LENGTH_SHORT).show()
            pickedFile = null
        }
        recorder = null
        isRecording = false
        val elapsed = System.currentTimeMillis() - recordStart
        if (elapsed < 1000) {
            Toast.makeText(ctx, "录音太短，请至少录制1秒", Toast.LENGTH_SHORT).show()
            pickedFile = null
        } else {
            pickedFile?.let { loadForPreview(it) }
        }
    }



    // 切换播放
    fun togglePlay() {
        playerState.value?.let { player ->
            val next = !player.playWhenReady
            player.playWhenReady = next
            isPlaying = next
        }
    }

    // 本地文件选择
    val filePicker = rememberLauncherForActivityResult(OpenDocument()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val type = ctx.contentResolver.getType(uri) ?: ""
        if (!type.startsWith("audio/")) {
            Toast.makeText(ctx, "请选择音频文件", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        copyUriToCache(ctx, uri)?.also {
            pickedFile = it
            loadForPreview(it)
        }
    }

    // 录音权限
    val permLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted) startRecord()
        else Toast.makeText(ctx, "录音权限被拒绝", Toast.LENGTH_SHORT).show()
    }

    // 实时更新录音时长
    LaunchedEffect(isRecording) {
        recordElapsed = 0L
        while (isRecording) {
            recordElapsed = System.currentTimeMillis() - recordStart
            delay(300)
        }
    }

    // 实时更新播放进度
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            playerState.value?.let { positionMs = it.currentPosition }
            delay(50)
        }
    }

    // 上传结果监听
    LaunchedEffect(uploadState) {
        when (uploadState) {
            is UploadState.Success -> {
                snackbarHost.showSnackbar("创建成功：${(uploadState as UploadState.Success).voiceId}")
                nav.popBackStack()
            }
            is UploadState.Error -> {
                snackbarHost.showSnackbar("创建失败：${(uploadState as UploadState.Error).msg}")
            }
            else -> {}
        }
    }

    // 计算并动画化滑块进度
    val rawRatio = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
    val sliderProgress by animateFloatAsState(
        targetValue = rawRatio.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300, easing = LinearEasing)
    )

    Scaffold(
        topBar = { TopAppBar(title = { Text("创建音色") }) },
        snackbarHost = { SnackbarHost(snackbarHost) }
    ) { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text("录制或选择一段音频，用以生成新的音色", style = MaterialTheme.typography.bodyMedium)

            // 按钮区域
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(
                    onClick = {
                        if (isRecording) stopRecord()
                        else permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Mic, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isRecording) "停止录音" else "开始录音")
                }
                OutlinedButton(
                    onClick = { filePicker.launch(arrayOf("audio/*")) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AudioFile, null)
                    Spacer(Modifier.width(8.dp))
                    Text("选择本地音频")
                }
            }

            // 录音时长指示
            if (isRecording) {
                val mm = TimeUnit.MILLISECONDS.toMinutes(recordElapsed)
                val ss = TimeUnit.MILLISECONDS.toSeconds(recordElapsed) % 60
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).background(Color.Red, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(String.format(Locale.getDefault(), "%02d:%02d", mm, ss))
                }
            }

            // 预览区
            if (pickedFile != null) {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("预览", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            IconButton(onClick = { togglePlay() }) {
                                Icon(
                                    if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    null
                                )
                            }
                            Slider(
                                value = sliderProgress,
                                onValueChange = { frac ->
                                    playerState.value?.seekTo((frac * durationMs).toLong())
                                    positionMs = (frac * durationMs).toLong()
                                },
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                String.format(
                                    Locale.getDefault(),
                                    "%02d:%02d / %02d:%02d",
                                    TimeUnit.MILLISECONDS.toMinutes(positionMs),
                                    TimeUnit.MILLISECONDS.toSeconds(positionMs) % 60,
                                    TimeUnit.MILLISECONDS.toMinutes(durationMs),
                                    TimeUnit.MILLISECONDS.toSeconds(durationMs) % 60
                                ),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // 上传按钮
            Button(
                onClick = {
                    pickedFile?.let {
                        vm.reset()
                        vm.uploadAndCreate(it, it.nameWithoutExtension.take(12))
                    }
                },
                enabled = pickedFile != null && uploadState is UploadState.Idle,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("上传并创建")
            }
        }

        // 上传遮罩
        if (uploadState is UploadState.Uploading) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Column(
                    Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(Modifier.height(12.dp))
                    Text("上传 / 创建中…", color = Color.White)
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { vm.reset() }) {
                        Text("取消", color = Color.White)
                    }
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { playerState.value?.release() }
    }
}

/** SAF Uri -> File */
private fun copyUriToCache(ctx: android.content.Context, uri: Uri): File? = try {
    val name = ctx.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
        ?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx)
            else "audio_${System.currentTimeMillis()}.tmp"
        } ?: "audio_${System.currentTimeMillis()}.tmp"
    val dest = File(ctx.cacheDir, name)
    ctx.contentResolver.openInputStream(uri)?.use { input ->
        dest.outputStream().use { input.copyTo(it) }
    }
    dest
} catch (e: Exception) {
    e.printStackTrace(); null
}
