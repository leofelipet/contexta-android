package com.leofelipet.contexta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ParseTest {
    @Test
    fun conversationPageCursor() {
        val page = contextaJson.decodeFromString<ConversationPage>(
            """{"data":[{"id":"c1","title":"Ana","last_message":{"text":"oi","type":"text"}}],"next_cursor":"abc"}""",
        )
        assertEquals("c1", page.data.single().id)
        assertEquals("Ana", page.data.single().displayTitle)
        assertEquals("oi", page.data.single().lastMessage?.displayText())
        assertEquals("abc", page.nextCursor)
    }

    @Test
    fun messageAudioFallsBackToTranscription() {
        val page = contextaJson.decodeFromString<MessagePage>(
            """{"data":[{"id":"m1","direction":"inbound","type":"audio","transcription":{"status":"done","text":" falou "}}]}""",
        )
        assertEquals("falou", page.data.single().displayText())
    }

    @Test
    fun updateIsNewerOnlyWhenRemoteVersionGrows() {
        val update = contextaJson.decodeFromString<AndroidUpdate>("""{"version_code":2,"version_name":"1.0.1"}""")
        assertTrue(needsUpdate(1, update.versionCode))
        assertFalse(needsUpdate(2, update.versionCode))
    }

    @Test
    fun baseUrlDropsApiSuffix() {
        assertEquals("https://contexta.example", normalizeBaseUrl("https://contexta.example/api/v1/"))
        assertEquals("https://contexta.example", normalizeBaseUrl("contexta.example"))
    }
}
