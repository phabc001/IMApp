package com.example.imapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.imapp.ui.component.rememberAudioPlayer
import com.example.imapp.viewmodel.VoiceCloneViewModel

/**
 * 声音克隆页面
 * 1. 输入文字  2. 选择音色  3. 调用 TTS 合成  4. 保存 / 插入音频列表
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCloneScreen(
    modifier: Modifier = Modifier,
    vm: VoiceCloneViewModel = viewModel()
) {
    /* ---------- 状态收集 ---------- */
    val voices  by vm.voices.collectAsState()
    val loading by vm.isLoading.collectAsState()
    val result  by vm.resultPath.collectAsState()

    /* ---------- 本地 UI 状态 ---------- */
    var text by remember { mutableStateOf("") }
    var expand by remember { mutableStateOf(false) }
    var chosen by remember { mutableStateOf(voices.firstOrNull()?.voiceId ?: "") }

    // 若列表变化而选中的 voice 不在其中，用首条兜底
    LaunchedEffect(voices) {
        if (voices.none { it.voiceId == chosen } && voices.isNotEmpty()) {
            chosen = voices.first().voiceId
        }
    }

    /* ---------- UI ---------- */
    Column(
        modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {

        /* 文字输入 */
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("输入要合成的文字") },
            minLines = 4
        )

        /* 音色下拉选择 */
        ExposedDropdownMenuBox(
            expanded = expand,
            onExpandedChange = { expand = !expand }
        ) {
            OutlinedTextField(
                value = voices.find { it.voiceId == chosen }?.displayName ?: "",
                onValueChange = {},
                readOnly = true,
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expand) },
                label = { Text("选择音色") },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(
                expanded = expand,
                onDismissRequest = { expand = false }
            ) {
                voices.forEach {
                    DropdownMenuItem(
                        text = { Text(it.displayName) },
                        onClick = {
                            chosen = it.voiceId
                            expand = false
                        }
                    )
                }
            }
        }

        /* 合成按钮 */
        Button(
            enabled = text.isNotBlank() && !loading,
            onClick = { vm.synthesize(text, chosen) },
            modifier = Modifier.fillMaxWidth()
        ) { Text(if (loading) "合成中…" else "开始合成") }

        /* 进度圈 */
        if (loading) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator()
                Text("正在调用 TTS 服务…")
            }
        }

        /* 合成结果 */
        result?.let { path ->
            OutlinedCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.outlinedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                val playAudio = rememberAudioPlayer(path)

                Column(Modifier.padding(16.dp)) {
                    Text("合成完成！文件已保存：")
                    Text(
                        path,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = { playAudio() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)   // ← 先放图标
                        Spacer(Modifier.width(8.dp))
                        Text("播放 / 分享")
                    }
                }
            }
        }
    }
}
