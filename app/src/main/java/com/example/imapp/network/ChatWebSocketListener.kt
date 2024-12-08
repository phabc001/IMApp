//ChatWebSocketListener.kt
package com.example.imapp.network

import android.util.Log
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class ChatWebSocketListener(
    private val onMessageReceived: (String) -> Unit
) : WebSocketListener() {



    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.i("ChatWebSocketListener", "WebSocket connected")
        WebSocketManager.onConnected()
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.i("ChatWebSocketListener", "Received message: $text")
        onMessageReceived(text) // 将接收到的消息传递给 UI 层
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        Log.i("ChatWebSocketListener", "Received bytes: ${bytes.hex()}")
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        Log.i("ChatWebSocketListener", "WebSocket closed: $reason")
        WebSocketManager.onDisconnected()
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        Log.e("ChatWebSocketListener", "WebSocket error: ${t.message}")
        WebSocketManager.onDisconnected()
    }
}
