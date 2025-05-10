package com.example.imapp.model

data class AiReplyRequest(val user_id: String, val message: String)
data class AiReplyResponse(val reply: AiReplyContent?)
data class AiReplyContent(val content: String)
