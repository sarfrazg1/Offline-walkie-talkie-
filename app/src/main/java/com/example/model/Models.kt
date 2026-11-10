package com.example.model

/**
 * Representation of a nearby walkie-talkie peer device.
 */
data class DeviceEndpoint(
    val id: String,
    val name: String,
    val isConnected: Boolean = false,
    val isConnecting: Boolean = false,
    val authDigits: String? = null,
    val latencyMs: Long? = null,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

/**
 * Current radio operational state for half-duplex communication.
 */
enum class RadioState {
    IDLE,          // Channel is clear, ready to transmit
    TRANSMITTING,  // Local user is holding PTT and streaming audio
    RECEIVING,     // A remote peer is currently transmitting audio
    BUSY,          // Channel is locked by another speaker or switching
    IN_CALL        // Full-duplex two-way voice call active
}

/**
 * States of an offline voice call session.
 */
enum class CallState {
    IDLE,
    OUTGOING_RINGING,
    INCOMING_RINGING,
    ACTIVE_CALL,
    ENDED
}

/**
 * Call type categorization for history and logging.
 */
enum class CallType {
    OUTGOING,
    INCOMING,
    MISSED,
    REJECTED
}

/**
 * Persisted call history record.
 */
data class CallRecord(
    val id: String,
    val peerName: String,
    val peerEndpointId: String,
    val type: CallType,
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0
)

/**
 * Active offline call session telemetry.
 */
data class ActiveCallInfo(
    val callId: String,
    val peerId: String,
    val peerName: String,
    val state: CallState = CallState.IDLE,
    val durationSeconds: Int = 0,
    val isMuted: Boolean = false,
    val isSpeakerphone: Boolean = true
)

/**
 * Audio recording and filtering configurations.
 */
data class AudioConfig(
    val sampleRate: Int = 16000,
    val enableEchoCancellation: Boolean = true,
    val enableNoiseSuppression: Boolean = true,
    val enableRogerBeep: Boolean = true,
    val micGainMultiplier: Float = 1.0f
)

/**
 * Persisted application settings.
 */
data class AppSettings(
    val callsign: String = "Alpha-1",
    val language: String = "en", // "en" for English, "ur" for Urdu
    val keepScreenOn: Boolean = true,
    val autoReconnect: Boolean = true
)

/**
 * Control message envelope for speaker lock, calling handshake, and ping telemetry over Nearby Connections.
 */
data class ControlMessage(
    val type: String,
    val senderId: String,
    val senderName: String,
    val payload: String = ""
) {
    companion object {
        // PTT Walkie-Talkie types
        const val TYPE_TALK_START = "TALK_START"
        const val TYPE_TALK_END = "TALK_END"
        const val TYPE_TALK_BUSY = "TALK_BUSY"
        const val TYPE_PING = "PING"
        const val TYPE_PONG = "PONG"

        // Full Offline Calling types
        const val TYPE_CALL_REQUEST = "CALL_REQ"
        const val TYPE_CALL_ACCEPT = "CALL_ACC"
        const val TYPE_CALL_REJECT = "CALL_REJ"
        const val TYPE_CALL_BUSY = "CALL_BUSY"
        const val TYPE_CALL_END = "CALL_END"
        
        fun serialize(msg: ControlMessage): ByteArray {
            return "${msg.type}|${msg.senderId}|${msg.senderName}|${msg.payload}".toByteArray(Charsets.UTF_8)
        }
        
        fun parse(bytes: ByteArray): ControlMessage? {
            return try {
                val str = String(bytes, Charsets.UTF_8)
                val parts = str.split("|", limit = 4)
                if (parts.size >= 3) {
                    ControlMessage(
                        type = parts[0],
                        senderId = parts[1],
                        senderName = parts[2],
                        payload = if (parts.size > 3) parts[3] else ""
                    )
                } else null
            } catch (e: Exception) {
                null
            }
        }
    }
}
