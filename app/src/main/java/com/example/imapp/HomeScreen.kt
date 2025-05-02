package com.example.imapp


import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.imapp.navigation.NavigationItem
import com.example.imapp.ui.screen.AudioManagerScreen
import com.example.imapp.ui.screen.ChatScreen
import com.example.imapp.ui.screen.MeScreen
import com.example.imapp.ui.screen.VoiceCloneScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.imapp.viewmodel.ChatViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController
) {
    val chatVm: ChatViewModel = viewModel()

    // 当前选中的Tab
    var selectedItem by remember { mutableStateOf(NavigationItem.Message) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(text = when (selectedItem) {
                        NavigationItem.Message -> "群聊"
                        NavigationItem.AudioManager -> "音频管理"
                        NavigationItem.VoiceClone   -> "声音克隆"
                        NavigationItem.Me -> "我"
                    })
                },
                /* 把右上角按钮放 actions */
                actions = {
                    if (selectedItem == NavigationItem.VoiceClone) {
                        IconButton(onClick = { navController.navigate("voice-management") }) {
                            Icon(Icons.Default.ManageAccounts, contentDescription = "音色管理")
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomNavigationBar(
                selectedItem = selectedItem,
                onSelectItem = { selectedItem = it }
            )
        }
    ) { innerPadding ->
        // 根据selectedItem决定显示哪个Screen
        when (selectedItem) {
            NavigationItem.Message -> {
                ChatScreen(
                    modifier = Modifier.padding(innerPadding),
                    viewModel = chatVm

                )
            }
            NavigationItem.AudioManager -> {
                AudioManagerScreen(
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavigationItem.VoiceClone   -> VoiceCloneScreen(
                Modifier.padding(innerPadding) )
            NavigationItem.Me -> {
                MeScreen(modifier = Modifier.padding(innerPadding))
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    selectedItem: NavigationItem,
    onSelectItem: (NavigationItem) -> Unit
) {
    NavigationBar {
        NavigationBarItem(
            selected = (selectedItem == NavigationItem.Message),
            onClick = { onSelectItem(NavigationItem.Message) },
            label = { Text("消息") },
            icon = {}
        )
        NavigationBarItem(
            selected = (selectedItem == NavigationItem.AudioManager),
            onClick = { onSelectItem(NavigationItem.AudioManager) },
            label = { Text("音频") },
            icon = {}
        )
        NavigationBarItem(
            selected = selectedItem == NavigationItem.VoiceClone,
            onClick  = { onSelectItem(NavigationItem.VoiceClone) },
            label    = { Text("克隆") },
            icon     = {}
        )
        NavigationBarItem(
            selected = (selectedItem == NavigationItem.Me),
            onClick = { onSelectItem(NavigationItem.Me) },
            label = { Text("我") },
            icon = {}
        )
    }
}


