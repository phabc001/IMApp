// ChatService.kt
package com.example.imapp.service

import android.app.Service
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Notification
import android.content.Intent
import android.os.IBinder
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.imapp.MainActivity
import com.example.imapp.R
import com.example.imapp.network.ChatWebSocketListener
import com.example.imapp.network.WebSocketManager
import com.example.imapp.repository.ChatRepository

class ChatService : Service() {
    private val CHANNEL_ID = "IMAppChatChannel"
    private val NOTIFY_ID = 1

    override fun onCreate() {
        super.onCreate()
        // 1. 创建通知渠道（适用于 Android 8.0+）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "IMApp 消息服务",
                NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "IMApp 持续运行，用于接收聊天消息"
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        // 2. 构建前台服务通知
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        )
        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("IMApp 正在运行")
            .setContentText("后台消息服务已启动")
//            .setSmallIcon(R.drawable.ic_notification) // 请确保有此图标资源
            .setSmallIcon(R.mipmap.ic_launcher)

            .setContentIntent(pendingIntent)
            .build()

        // 3. 将服务提升为前台服务
        startForeground(NOTIFY_ID, notification)

        // 4. 初始化 WebSocket 连接并设置回调
        WebSocketManager.setOnMessageReceivedCallback { message ->
            // 收到消息时，交由 Repository 处理（转发给 UI）
            ChatRepository.onReceiveMessage(message)
        }
        WebSocketManager.connect(ChatWebSocketListener { msg ->
            // WebSocketListener 回调，触发 Repository 内全局回调
            WebSocketManager.onMessageReceived?.invoke(msg)
        })
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 服务被显式启动后的后续逻辑（此处可选，onCreate已处理连接）
        return START_STICKY  // 确保服务异常终止后系统重启服务
    }

    override fun onBind(intent: Intent?): IBinder? {
        // 不提供绑定
        return null
    }

    override fun onDestroy() {
        super.onDestroy()
        // 服务销毁时关闭 WebSocket 连接
        WebSocketManager.close()
    }
}
