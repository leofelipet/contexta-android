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
    fun githubReleaseIsNewerByTag() {
        val release = contextaJson.decodeFromString<GithubRelease>(
            """{"tag_name":"v1.1.0","assets":[{"name":"app-release.apk","browser_download_url":"https://example/app.apk"}]}""",
        )
        assertEquals("1.1.0", release.versionName)
        assertEquals("https://example/app.apk", release.apkUrl)
        assertTrue(needsUpdate("1.0.0", release.versionName))
        assertFalse(needsUpdate("1.1.0", release.versionName))
        assertTrue(needsUpdate("1.9.0", "1.10.0"))
    }

    @Test
    fun baseUrlDropsApiSuffix() {
        assertEquals("https://contexta.example", normalizeBaseUrl("https://contexta.example/api/v1/"))
        assertEquals("https://contexta.example", normalizeBaseUrl("contexta.example"))
    }
}
