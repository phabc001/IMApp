//ChatWebSocketListener.kt
package com.example.imapp.network

import android.util.Log
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import org.json.JSONObject


class ChatWebSocketListener(
    private val onMessageReceived: (Message) -> Unit
) : WebSocketListener() {

    override fun onOpen(webSocket: WebSocket, response: Response) {
        Log.i("ChatWebSocketListener", "WebSocket connected")
        WebSocketManager.onConnected()
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        Log.i("ChatWebSocketListener", "Received message: $text")
        try {
            // 假设收到的消息是 JSON 格式
            val jsonObject = JSONObject(text)
            val message = Message.fromJson(jsonObject) // 解析为 Message 对象
            onMessageReceived(message) // 将解析后的消息对象传递给 UI
        } catch (e: Exception) {
            Log.e("ChatWebSocketListener", "Error decoding message: ${e.message}")
            // 如果解析失败，可以传递一个系统消息或原始文本
            val errorMessage = Message.SystemMessage(
                id = "error-${System.currentTimeMillis()}",
                timestamp = System.currentTimeMillis(),
                sender = "System",
                receiver = "Client",
                info = "Failed to decode message: $text"
            )
            onMessageReceived(errorMessage)
        }
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
