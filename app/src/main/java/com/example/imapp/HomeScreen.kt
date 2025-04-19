package com.example.imapp

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.flow.MutableStateFlow
import com.example.imapp.navigation.NavigationItem
import com.example.imapp.network.Message
import com.example.imapp.ui.screen.AudioManagerScreen
import com.example.imapp.ui.screen.ChatScreen
import com.example.imapp.ui.screen.MeScreen
import com.example.imapp.ui.screen.VoiceCloneScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    messageFlow: MutableStateFlow<List<Message>>,
    onSendText: (String) -> Unit,
    onStartRecord: () -> Unit,
    onStopRecord: () -> Unit,
    onSendVoice: () -> Unit,
    onRequestAiReply: (String) -> Unit,
    onRequestTts: (String) -> Unit
) {
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
                    messageFlow = messageFlow,
                    onSendText = onSendText,
                    onStartRecord = onStartRecord,
                    onStopRecord = onStopRecord,
                    onSendVoice = onSendVoice,
                    onRequestAiReply = onRequestAiReply,
                    onRequestTts = onRequestTts
                )
            }
            NavigationItem.AudioManager -> {
                AudioManagerScreen(
                    modifier = Modifier.padding(innerPadding)
                )
            }
            NavigationItem.VoiceClone   -> VoiceCloneScreen( Modifier.padding(innerPadding) )
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


@Preview(showBackground = true, name = "HomeScreenPreview")
@Composable
fun HomeScreenPreview() {
    // 1. 构造一个 mock 的消息 Flow
    val mockMessagesFlow = remember {
        MutableStateFlow(
            listOf(
                Message.TextMessage(sender = "AndroidApp", text = "Hello from app"),
                Message.TextMessage(sender = "ChromePlugin", text = "Hi, I'm plugin"),
                Message.TextMessage(sender = "AI", text = "AI auto reply sample..."),
            )
        )
    }

    // 2. 调用 HomeScreen，传入空实现的回调
    HomeScreen(
        messageFlow = mockMessagesFlow as MutableStateFlow<List<Message>>,
        onSendText = { },
        onStartRecord = { },
        onStopRecord = { },
        onSendVoice = { },
        onRequestAiReply = {},
        onRequestTts = {}
    )
}