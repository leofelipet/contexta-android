package com.leofelipet.contexta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

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

    @Test
    fun dayChipFollowsSaoPaulo() {
        val zone = ZoneId.of("America/Sao_Paulo")
        val today = LocalDate.of(2026, 11, 11)
        assertEquals("12:00", formatClock("2026-11-11T15:00:00Z", zone))
        assertEquals("Hoje", dayLabel("2026-11-11T15:00:00Z", today, zone))
        assertEquals("Ontem", dayLabel("2026-11-11T02:00:00Z", today, zone))
        assertEquals("9 de novembro", dayLabel("2026-11-09T15:00:00Z", today, zone))
        assertEquals("11 de novembro de 2025", dayLabel("2025-11-11T15:00:00Z", today, zone))
        assertTrue(showDayChip("2026-11-11T15:00:00Z", null, zone))
        assertFalse(showDayChip("2026-11-11T15:00:00Z", "2026-11-11T18:00:00Z", zone))
        assertTrue(showDayChip("2026-11-11T15:00:00Z", "2026-11-10T15:00:00Z", zone))
    }

    @Test
    fun prefetchStartsAtTheFifteenthRow() {
        assertFalse(shouldPrefetch(13, 20))
        assertTrue(shouldPrefetch(14, 20))
        assertFalse(shouldPrefetch(33, 40))
        assertTrue(shouldPrefetch(34, 40))
        assertTrue(shouldPrefetch(9, 10))
        assertFalse(shouldLoadOlder(2, 20))
        assertTrue(shouldLoadOlder(17, 20))
    }
}
