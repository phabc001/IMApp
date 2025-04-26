package com.example.imapp.ui.screen

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts.OpenMultipleDocuments
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.example.imapp.viewmodel.AudioManagerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioManagerScreen(
    modifier: Modifier = Modifier,
    vm: AudioManagerViewModel = viewModel()
) {
    val ctx      = LocalContext.current
    val scope    = rememberCoroutineScope()

    /* ---- 支持多选的系统文件选择器 ---- */
    val multiLauncher = rememberLauncherForActivityResult(OpenMultipleDocuments()) { uris: List<Uri> ->
        if (uris.isNotEmpty()) scope.launch { vm.importList(ctx, uris) }
    }

    /* ---- 状态收集 ---- */
    val list      by vm.audioList.collectAsState()
    var isPlaying by remember { mutableStateOf(false) }
    var loopMode  by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("音频管理") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { multiLauncher.launch(arrayOf("audio/*")) }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { inner ->
        Box(Modifier.fillMaxSize().padding(inner)) {
            if (list.isEmpty()) {
                /* ---- 空列表占位 ---- */
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement  = Arrangement.Center
                ) {
                    Text(text = "暂无音频", textAlign = TextAlign.Center)
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = { multiLauncher.launch(arrayOf("audio/*")) }) { Text("导入或录音") }
                }
            } else {
                /* ---- 音频列表 ---- */
                LazyColumn {
                    items(list) { item ->
                        AudioRow(
                            item    = item,
                            playing = vm.playingItem.collectAsState().value?.id == item.id,
                            onCheck = { vm.toggleSelect(item.id) },
                            onDelete = {vm.delete(item.id)}

                        )
                    }
                }
            }

            /* ---- 底部播放/循环控制 ---- */
            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("循环")
                Switch(loopMode, onCheckedChange = { loopMode = it })
                IconButton(onClick = {
                    if (isPlaying) vm.pause() else vm.play(loopMode)
                    isPlaying = !isPlaying
                }) {
                    Icon(if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow, null)
                }
            }
        }
    }
}
/* 单行条目 */
@Composable
fun AudioRow(
    item: com.example.imapp.data.AudioItem,
    playing: Boolean,
    onCheck: () -> Unit,
    onDelete: () -> Unit
) {
    ListItem(
        headlineContent   = { Text(item.name) },
        supportingContent = { if (playing) Text("播放中") },
        leadingContent    = { Checkbox(checked = item.selected, onCheckedChange = { onCheck() }) },
        trailingContent   = { IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null) } }
    )
}