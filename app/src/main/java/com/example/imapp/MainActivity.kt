//MainActivity.kt
package com.example.imapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.imapp.network.WebSocketManager
import com.example.imapp.network.ChatWebSocketListener
import com.example.imapp.ui.theme.IMAppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items


import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.material3.ButtonDefaults


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 用于存储接收到的消息
        val messageFlow = MutableStateFlow<List<String>>(emptyList())


        // Initialize WebSocket
        val webSocketListener = ChatWebSocketListener { message ->
            // 将接收到的消息添加到消息列表中
            messageFlow.update { currentMessages ->
                listOf(message) + currentMessages
            }
        }

        WebSocketManager.setOnMessageReceivedCallback { message ->
            messageFlow.update { listOf(message) + it }
        }


        WebSocketManager.connect(webSocketListener)

        setContent {
            IMAppTheme {
                WebSocketScreen(messageFlow)
            }
        }
    }
}



@Composable
fun WebSocketScreen(messageFlow: MutableStateFlow<List<String>>) {
    val messages by messageFlow.collectAsState() // 观察消息流的变化
    var inputMessage by remember { mutableStateOf("") } // 用于存储用户输入

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        // 显示消息列表
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            items(messages) { message ->Log.d("WebSocketScreen", "Rendering message: $message") // 检查是否有消息被渲染

                Text(
                    text = message,
                    fontSize = 16.sp,
                    color = Color.Black,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }

        // 输入框和发送按钮
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = inputMessage,
                onValueChange = { inputMessage = it },
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
                    .height(50.dp)
                    .background(Color.LightGray) // 设置背景色，避免和页面背景一致
                    .padding(8.dp), // 内边距


                maxLines = 1,
                singleLine = true
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Blue,
                    contentColor = Color.White
                ),
                onClick = {
                    if (inputMessage.isNotBlank()) {
                        Log.d("WebSocketScreen", "Sending message: $inputMessage")
                        WebSocketManager.sendMessage(inputMessage)
                        inputMessage = "" // 发送后清空输入框
                    }
                    else {
                        Log.e("WebSocketScreen", "Message is blank, not sending")
                    }
                }
            ) {
                Text("Send")
            }
        }

        // 关闭 WebSocket 按钮
        Button(
            onClick = {
                WebSocketManager.close()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Close Connection")
        }
    }
}


@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    // 使用一个模拟的消息流
    val mockMessageFlow = MutableStateFlow(
        listOf("Welcome to Chat!", "This is a preview message.")
    )
    IMAppTheme {
        WebSocketScreen(mockMessageFlow)
    }
}
