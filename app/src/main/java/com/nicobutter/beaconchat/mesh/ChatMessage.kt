package com.nicobutter.beaconchat.mesh

/**
 * Represents a chat message exchanged between BeaconChat devices in the mesh network.
 *
 * This data class encapsulates all information about a message, including sender details,
 * content, timestamp, and whether the message was sent by the current user. Messages
 * are exchanged via Bluetooth LE GATT characteristics.
 *
 * @property senderId Unique identifier (Bluetooth address) of the sending device
 * @property senderName Display name or callsign of the message sender
 * @property recipientId Unique identifier of the recipient device (empty for broadcasts)
 * @property content The actual message text content
 * @property timestamp When the message was sent/received (milliseconds since epoch)
 * @property isFromMe Whether this message was sent by the current user (affects UI display)
 */
data class ChatMessage(
        val senderId: String, // Address of the sender
        val senderName: String, // Name/Callsign of the sender
        val recipientId: String = "", // Address of recipient (empty if broadcast)
        val content: String,
        val timestamp: Long,
        val isFromMe: Boolean
)
