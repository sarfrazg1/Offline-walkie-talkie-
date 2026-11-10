package com.example

import com.example.config.AppConfig
import com.example.model.ControlMessage
import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests verifying radio control protocols and configuration validity.
 */
class ExampleUnitTest {
    @Test
    fun controlMessage_serialization_and_parsing_isCorrect() {
        val original = ControlMessage(
            type = ControlMessage.TYPE_TALK_START,
            senderId = "test_endpoint_123",
            senderName = "Alpha-Leader",
            payload = "seq_1"
        )
        val serialized = ControlMessage.serialize(original)
        val parsed = ControlMessage.parse(serialized)

        assertNotNull(parsed)
        assertEquals(original.type, parsed?.type)
        assertEquals(original.senderId, parsed?.senderId)
        assertEquals(original.senderName, parsed?.senderName)
        assertEquals(original.payload, parsed?.payload)
    }

    @Test
    fun developerCredentials_areCorrect() {
        assertEquals("Rao Muhammad Sarfraz", AppConfig.DEVELOPER_NAME)
        assertEquals("+923360074976", AppConfig.WHATSAPP_NUMBER)
        assertTrue(AppConfig.WHATSAPP_LINK.contains("923360074976"))
    }
}
