//wWebSocketManager.kt
package com.example.imapp.network

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*

object WebSocketManager {
    private const val SOCKET_URL = "wss://beforeai.net/ws/chat/x/" // 替换为你的 WebSocket 地址
    private var webSocket: WebSocket? = null
    private var isConnected = false // 标记连接状态
    private var isConnecting = false // 标记是否正在连接
    private var retryCount = 0 // 当前重连次数
    private val retryDelay = 1000L // 重连间隔（毫秒）

    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    private val coroutineScope = CoroutineScope(Dispatchers.IO) // 使用 IO 线程的协程作用域

    var onMessageReceived: ((Message) -> Unit)? = null // 新增全局消息回调

    /**
     * 建立 WebSocket 连接
     * @param listener 自定义的 WebSocketListener
     */
    fun connect(listener: WebSocketListener) {
        if (isConnected || isConnecting) {
            Log.i("WebSocketManager", "WebSocket 已连接或正在连接，跳过此次连接尝试")
            return
        }

        isConnecting = true
        val request = Request.Builder().url(SOCKET_URL).build()
        webSocket = client.newWebSocket(request, listener)
        Log.i("WebSocketManager", "Connecting to WebSocket: $SOCKET_URL")
    }

    /**
     * 发送消息
     * @param message 要发送的消息对象
     */
    fun sendMessage(message: Message) {
        if (!isConnected) {
            Log.e("WebSocketManager", "WebSocket 未连接，无法发送消息")
            return
        }

        val jsonMessage = Message.toJson(message)
        webSocket?.send(jsonMessage)
        Log.i("WebSocketManager", "Sent message: $jsonMessage")
    }

    /**
     * 关闭 WebSocket 连接
     */
    fun close() {
        if (webSocket == null) {
            Log.e("WebSocketManager", "WebSocket 未连接，无法关闭")
            return
        }

        webSocket?.close(1000, "Client closed connection")
        isConnected = false
        isConnecting = false
        Log.i("WebSocketManager", "Closing WebSocket connection")
    }

    /**
     * WebSocket 连接成功时调用
     */
    fun onConnected() {
        isConnected = true
        isConnecting = false
//        retryCount = 0 // 重置重连计数
        Log.i("WebSocketManager", "WebSocket 已连接")
    }

    /**
     * WebSocket 连接关闭时调用
     */
    fun onDisconnected() {
        isConnected = false
        retryCount++
        Log.w("WebSocketManager", "尝试重新连接 WebSocket，第 $retryCount 次...")
        scheduleReconnect()
    }

    /**
     * 使用 Coroutine 进行非阻塞延迟重连
     * @param listener 自定义的 WebSocketListener
     */
    private fun scheduleReconnect() {
        coroutineScope.launch {
            delay(retryDelay)
            /*
            * 新建的 ChatWebSocketListener 实例通过 { message -> onMessageReceived?.invoke(message) } 绑定了全局的消息回调。
            * */
            connect(ChatWebSocketListener { message ->
                onMessageReceived?.invoke(message)
            })
        }
    }

    fun setOnMessageReceivedCallback(callback: (Message) -> Unit) {
        onMessageReceived = callback
    }
}
