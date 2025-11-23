package com.nicobutter.beaconchat.mesh

data class ChatMessage(
        val senderId: String, // Address of the sender
        val senderName: String, // Name/Callsign of the sender
        val content: String,
        val timestamp: Long,
        val isFromMe: Boolean
)
