package com.example.imapp.network

import org.json.JSONObject

// 消息的父类，封装通用属性和行为
sealed class Message {
    abstract val type: String // 消息类型：text、audio 或 system
    abstract val id: String // 消息唯一 ID
    abstract val timestamp: Long // 消息发送时间戳
    abstract val sender: String // 消息发送者 ID
    abstract val receiver: String // 消息接收者 ID

    // 文本消息
    data class TextMessage(
        override val id: String = generateId(), // 默认生成唯一 ID,
        override val timestamp: Long = System.currentTimeMillis(),
        override val sender: String,
        override val receiver: String = "all",
        val text: String // 文本内容
    ) : Message() {
        override val type: String = "text" // 消息类型为文本
    }

    // 音频消息
    data class AudioMessage(
        override val id: String = generateId(),
        override val timestamp: Long = System.currentTimeMillis(),
        override val sender: String,
        override val receiver: String = "all",
        val data: String, // 音频数据（Base64 编码）
        val duration: Int, // 音频时长（单位：秒）
        val format: String // 音频格式（如 mp3、wav）
    ) : Message() {
        override val type: String = "audio" // 消息类型为音频
    }

    // 系统消息（如通知信息）
    data class SystemMessage(
        override val id: String = generateId(),
        override val timestamp: Long = System.currentTimeMillis(),
        override val sender: String = "System",
        override val receiver: String = "all",
        val info: String // 系统消息内容
    ) : Message() {
        override val type: String = "system" // 消息类型为系统消息
    }

    // 伴生对象，用于 JSON 和 Message 对象之间的转换
    companion object {

        // 用于生成唯一 ID 的工具函数
        private fun generateId(): String = java.util.UUID.randomUUID().toString()

        // 从 JSON 格式解析为 Message 对象
        fun fromJson(json: JSONObject): Message {
            val type = json.getString("type") // 获取消息类型
            val id = json.getString("id") // 获取消息 ID
            val timestamp = json.getLong("timestamp") // 获取时间戳
            val sender = json.getString("sender") // 获取发送者 ID
            val receiver = json.getString("receiver") // 获取接收者 ID
            val content = json.getJSONObject("content")


            // 根据消息类型解析具体的消息内容
            return when (type) {
                "text" -> TextMessage(
                    id = id,
                    timestamp = timestamp,
                    sender = sender,
                    receiver = receiver,
                    text =content.getString("text")

                )
                "audio" -> AudioMessage(
                    id = id,
                    timestamp = timestamp,
                    sender = sender,
                    receiver = receiver,
                    data = content.getString("data"), // 获取音频数据
                    duration = content.getInt("duration"), // 获取音频时长
                    format = content.getString("format")// 获取音频格式
                )
                "system" -> SystemMessage(
                    id = id,
                    timestamp = timestamp,
                    sender = sender,
                    receiver = receiver,
                    info = content.getString("info")// 获取系统消息内容
                )
                else -> throw IllegalArgumentException("Unknown message type: $type") // 异常：未知的消息类型
            }
        }

        // 将 Message 对象转换为 JSON 格式字符串
        fun toJson(message: Message): String {
            val json = JSONObject()
            json.put("type", message.type) // 放入消息类型
            json.put("id", message.id) // 放入消息 ID
            json.put("timestamp", message.timestamp) // 放入时间戳
            json.put("sender", message.sender) // 放入发送者 ID
            json.put("receiver", message.receiver) // 放入接收者 ID

            val content = JSONObject()
            when (message) {
                is TextMessage -> content.put("text", message.text) // 放入文本内容
                is AudioMessage -> {
                    content.put("data", message.data) // 放入音频数据
                    content.put("duration", message.duration) // 放入音频时长
                    content.put("format", message.format) // 放入音频格式
                }
                is SystemMessage -> content.put("info", message.info) // 放入系统消息内容
            }
            json.put("content", content) // 将内容放入消息中
            return json.toString() // 转换为字符串
        }
    }
}
