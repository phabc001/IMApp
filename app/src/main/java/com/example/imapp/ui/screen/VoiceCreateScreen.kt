package com.example.imapp.ui.screen

import android.Manifest
import android.content.Context
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
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.imapp.viewmodel.VoiceManagementViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCreateScreen(
    nav: NavController,
    vm: VoiceManagementViewModel = viewModel()
) {
    /* 环境 */
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    /* 录音状态 */
    var isRecording by remember { mutableStateOf(false) }
    var progress     by remember { mutableStateOf(false) }
    var recorder     by remember { mutableStateOf<MediaRecorder?>(null) }
    var recordFile   by remember { mutableStateOf<File?>(null) }

    /* ---------- 录音函数 ---------- */
    fun startRecord() {
        val cache = File(ctx.cacheDir, "voice_${UUID.randomUUID()}.m4a")
        recorder = MediaRecorder().apply {
            setAudioSource(MediaRecorder.AudioSource.MIC)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setOutputFile(cache.absolutePath)
            prepare()
            start()
        }
        recordFile = cache
        isRecording = true
    }

    fun stopRecordAndUpload() {
        try {
            recorder?.apply {
                try { stop() } catch (_: RuntimeException) { recordFile?.delete() }
                release()
            }
        } catch (_: Exception) {}
        recorder = null
        isRecording = false

        recordFile?.let { file ->
            if (file.exists() && file.length() > 1024) {
                scope.launch {
                    progress = true
                    withContext(Dispatchers.IO) { vm.create(file.absolutePath) }
                    progress = false
                    nav.popBackStack()
                }
            } else {
                Toast.makeText(ctx, "录音失败，请再试一次", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /* ---------- SAF 文件选择器 ---------- */
    val filePicker = rememberLauncherForActivityResult(OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val type = ctx.contentResolver.getType(uri) ?: ""
        if (!type.startsWith("audio/")) {
            Toast.makeText(ctx, "请选择音频文件", Toast.LENGTH_SHORT).show()
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            progress = true
            val file = copyUriToCache(ctx, uri)
            withContext(Dispatchers.IO) { vm.create(file.absolutePath) }
            progress = false
            nav.popBackStack()
        }
    }

    /* ---------- 录音权限 ---------- */
    val permLauncher = rememberLauncherForActivityResult(RequestPermission()) { granted ->
        if (granted) startRecord()
        else Toast.makeText(ctx, "录音权限被拒绝", Toast.LENGTH_SHORT).show()
    }

    /* ---------- UI ---------- */
    Scaffold(topBar = { TopAppBar(title = { Text("创建音色") }) }) { pad ->
        Column(
            Modifier
                .padding(pad)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {

            /* 录音按钮 */
            OutlinedButton(
                onClick = {
                    if (isRecording) stopRecordAndUpload()
                    else permLauncher.launch(Manifest.permission.RECORD_AUDIO)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Mic, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (isRecording) "停止并上传" else "开始录音")
            }

            /* 文件选择按钮 */
            OutlinedButton(
                onClick = { filePicker.launch(arrayOf("audio/*")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.AudioFile, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("从文件选择音频")
            }

            /* 进度指示 */
            if (progress) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator()
                    Spacer(Modifier.width(12.dp))
                    Text("上传 / 创建中…")
                }
            }
        }
    }
}

/* ---------- 工具函数：复制 Uri -> 缓存 ---------- */
private fun copyUriToCache(ctx: Context, uri: Uri): File {
    val name = ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
        val idx = c.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        c.moveToFirst(); c.getString(idx)
    } ?: "audio_${UUID.randomUUID()}.tmp"

    val dest = File(ctx.cacheDir, name)
    ctx.contentResolver.openInputStream(uri)?.use { input ->
        dest.outputStream().use { input.copyTo(it) }
    }
    return dest
}
