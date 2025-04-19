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
    private var isConnected: Boolean = false      // 当前连接是否已建立
    private var isConnecting: Boolean = false     // 是否正在连接中（防止重复连接请求）
    private var retryCount: Int = 0               // 重连次数计数
    private var baseRetryDelay = 1000L            // 初始重连间隔1秒
    private var currentRetryDelay = baseRetryDelay// 当前重连延迟


    // OkHttpClient 设置：含ping间隔和超时配置
    private val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)            // 连接超时
            .readTimeout(0, TimeUnit.SECONDS)                // 读取超时设为0，以让pingInterval接管
            .pingInterval(30, TimeUnit.SECONDS)              // 每30秒发送一次Ping心跳
            .build()
    }

    // 协程作用域：用于调度重连等异步任务
    private val scope = CoroutineScope(Dispatchers.IO)
    var onMessageReceived: ((Message) -> Unit)? = null // 新增全局消息回调

    /** 设置全局消息接收回调 */
    fun setOnMessageReceivedCallback(callback: (Message) -> Unit) {
        onMessageReceived = callback
    }

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
        if (!isConnected || webSocket == null) {
            Log.e("WebSocketManager", "发送失败：WebSocket未连接")
            return
        }
        val jsonStr = Message.toJson(message)
        val success = webSocket?.send(jsonStr) ?: false
        if (success) {
            Log.i("WebSocketManager", "已发送消息: $jsonStr")
        } else {
            Log.e("WebSocketManager", "消息发送失败")
        }
    }


    /** 关闭连接 */
    fun close() {
        if (webSocket != null) {
            webSocket?.close(1000, "Client closed")  // 正常关闭码1000
            Log.i("WebSocketManager", "WebSocket连接已关闭")
        } else {
            Log.w("WebSocketManager", "WebSocket未连接，无需关闭")
        }
        isConnected = false
        isConnecting = false
    }


    /**
     * WebSocket 连接成功时调用
     */
    fun onConnected() {
        isConnected = true
        isConnecting = false

        // 重置重连策略计数和延迟
        retryCount = 0
        currentRetryDelay = baseRetryDelay
        Log.i("WebSocketManager", "WebSocket连接成功")
        Log.i("WebSocketManager", "WebSocket 已连接")
    }

    /**
     * WebSocket 连接关闭时调用
     */
    fun onDisconnected() {
        isConnected = false
        retryCount++
        Log.w("WebSocketManager", "WebSocket断开，准备重连... (第${retryCount + 1}次)")
        scheduleReconnect()
    }

    /** 安排重连：使用协程延迟执行，避免阻塞线程 */
    private fun scheduleReconnect() {
        // 如果已经达到一定重连次数，可在此加入终止条件，例如超过10次则放弃或者提示用户检查网络
        scope.launch {
            retryCount++
            // 指数退避策略计算当前延迟：初始1s，之后逐次*2，设定上限（例如30s）
            currentRetryDelay = if (retryCount == 1) baseRetryDelay
            else (currentRetryDelay * 2).coerceAtMost(30000L)
            Log.i("WebSocketManager", "将在 ${currentRetryDelay} ms 后尝试第${retryCount}次重连")
            delay(currentRetryDelay)
            // 发起重连（使用新的监听器实例）
            connect(ChatWebSocketListener { message ->
                onMessageReceived?.invoke(message)
            })
        }
    }
}
