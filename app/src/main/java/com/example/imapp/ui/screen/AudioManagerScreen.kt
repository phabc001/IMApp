package com.example.imapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.imapp.viewmodel.AudioManagerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioManagerScreen(
    modifier: Modifier = Modifier,
    vm: AudioManagerViewModel = viewModel()
) {
    val list     by vm.audioList.collectAsState()
    val playing  by vm.playingItem.collectAsState()
    var loopMode by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { vm.loadAudios() }

    Scaffold(topBar = { TopAppBar(title = { Text("音频管理") }) }) { pad ->
        Column(modifier.padding(pad)) {

            /* -------- 音频列表 -------- */
            LazyColumn(Modifier.weight(1f)) {
                items(list, key = { it.id }) { item ->
                    AudioRow(
                        item     = item,
                        playing  = item.id == playing?.id,
                        onCheck  = { vm.toggleSelect(item.id) }
                    )
                }
            }

            /* -------- 底部播放控制栏 -------- */
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("循环")
                    Switch(loopMode, onCheckedChange = { loopMode = it })
                }
                IconButton(
                    onClick = {
                        if (isPlaying) vm.pause() else vm.play(loopMode)
                        isPlaying = !isPlaying
                    }
                ) {
                    Icon(
                        if (isPlaying) Icons.Default.Pause
                        else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                }
            }
        }
    }
}

/* 单行条目 */
@Composable
private fun AudioRow(
    item: com.example.imapp.data.AudioItem,
    playing: Boolean,
    onCheck: () -> Unit
) {
    ListItem(
        headlineContent = { Text(item.name) },
        leadingContent  = {
            IconButton(onClick = onCheck) {
                Icon(
                    if (item.selected) Icons.Default.CheckBox
                    else Icons.Default.CheckBoxOutlineBlank,
                    contentDescription = null
                )
            }
        },
        trailingContent = { if (playing) Text("播放中") },
        modifier        = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
    )
}
