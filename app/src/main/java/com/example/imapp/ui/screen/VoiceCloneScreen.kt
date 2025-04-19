package com.example.imapp.ui.screen
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.imapp.viewmodel.VoiceCloneViewModel
import androidx.lifecycle.viewmodel.compose.viewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCloneScreen(
    modifier: Modifier = Modifier,
    vm: VoiceCloneViewModel = viewModel()
) {
    val voices by vm.voices.collectAsState()
    val loading by vm.isLoading.collectAsState()
    val result by vm.resultPath.collectAsState()

    var text by remember { mutableStateOf("") }
    var expand by remember { mutableStateOf(false) }
    var chosen by remember { mutableStateOf(voices.first().voiceId) }

    Column(modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = text,
            onValueChange = { text = it },
            label = { Text("输入文字") },
            modifier = Modifier.fillMaxWidth()
        )
        ExposedDropdownMenuBox(expanded = expand, onExpandedChange = { expand = !expand }) {
            OutlinedTextField(
                value = voices.find { it.voiceId == chosen }?.displayName ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("音色") },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            ExposedDropdownMenu(expanded = expand, onDismissRequest = { expand = false }) {
                voices.forEach {
                    DropdownMenuItem(text = { Text(it.displayName) }, onClick = {
                        chosen = it.voiceId
                        expand = false
                    })
                }
            }
        }
        Button(
            enabled = text.isNotBlank() && !loading,
            onClick = { vm.synthesize(text, chosen) }
        ) { Text(if (loading) "合成中…" else "开始合成") }

        result?.let {
            Text("已生成：$it")
            Button(onClick = { /* TODO 打开/分享 或保存到系统媒体库 */ }) {
                Text("保存/分享")
            }
        }
    }
}
