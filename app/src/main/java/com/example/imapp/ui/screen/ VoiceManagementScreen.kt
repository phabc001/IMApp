package com.example.imapp.ui.screen

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.imapp.viewmodel.VoiceManagementViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.getValue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceManagementScreen(
    nav: NavController,
    vm: VoiceManagementViewModel = viewModel()
) {
    val voices by vm.voices.collectAsState()

    Scaffold(
        topBar = { TopAppBar(title = { Text("音色管理") }) },
        floatingActionButton = {
            FloatingActionButton(onClick = { nav.navigate("voice-create") }) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { pad ->
        LazyColumn(
            Modifier
                .padding(pad)
                .fillMaxSize()
        ) {
            items(voices) { v ->
                ListItem(
                    headlineContent = { Text(v.displayName) },
                    supportingContent = { Text(v.voiceId) },
                    trailingContent = {
                        if (!v.isDefault)
                            IconButton(onClick = { vm.delete(v.voiceId) }) {
                                Icon(Icons.Default.Delete, null)
                            }
                    }
                )
                Divider()
            }
        }
    }
}

