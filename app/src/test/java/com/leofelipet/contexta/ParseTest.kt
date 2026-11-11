package com.leofelipet.contexta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
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
    fun conversationBlockedByIdOrContact() {
        val page = contextaJson.decodeFromString<DenylistPage>(
            """{"data":[{"id":"d1","target_type":"conversation","target_id":"c1"},{"id":"d2","target_type":"contact","target_id":"p9"}]}""",
        )
        val (conversations, contacts) = denylistSets(page.data)
        val blocked = Conversation(id = "c1", title = "Ana")
        val viaContact = Conversation(id = "c2", title = "Bo", contactId = "p9")
        val open = Conversation(id = "c3", title = "Ca", type = "group")
        assertTrue(blocked.isBlocked(conversations, contacts))
        assertTrue(viaContact.isBlocked(conversations, contacts))
        assertFalse(open.isBlocked(conversations, contacts))
        assertTrue(open.isGroup)
    }

    @Test
    fun messageAudioFallsBackToTranscription() {
        val page = contextaJson.decodeFromString<MessagePage>(
            """{"data":[{"id":"m1","direction":"inbound","type":"audio","transcription":{"status":"done","text":" falou "}}]}""",
        )
        assertEquals("falou", page.data.single().displayText())
        assertEquals("Transcrição do áudio", page.data.single().audioLabel())
    }

    @Test
    fun contactNameFallsBackAndSearchKeepsConversation() {
        val page = contextaJson.decodeFromString<ContactPage>(
            """{"data":[{"id":"p1","name":"5511999","phone":"5511999","push_name":"Ana","company_name":"ACME"}]}""",
        )
        assertEquals("Ana", page.data.single().displayName)
        assertEquals("ACME", page.data.single().companyName)
        val unnamed = contextaJson.decodeFromString<Contact>("""{"id":"p2","phone":"119"}""")
        assertEquals("Contato sem nome", unnamed.displayName)
        val hit = contextaJson.decodeFromString<Message>(
            """{"id":"m9","direction":"outbound","text":"ok","conversation_id":"c1"}""",
        )
        assertEquals("c1", hit.conversationId)
        assertFalse(hit.inbound)
    }

    @Test
    fun taskOverdueIgnoresClosed() {
        val open = contextaJson.decodeFromString<Task>(
            """{"id":"t1","title":"Ligar","status":"pending","due_at":"2020-01-01","company_name":"ACME"}""",
        )
        val done = open.copy(status = "done")
        val today = LocalDate.of(2026, 11, 11)
        assertTrue(open.isOverdue(today))
        assertFalse(done.isOverdue(today))
        assertFalse(open.copy(dueAt = "2026-11-11").isOverdue(today))
        assertEquals("Pendente", taskStatusLabel("pending"))
        assertEquals("Concluída", taskStatusLabel("done"))
    }

    @Test
    fun reminderSendAtRoundsUpFiveMinutes() {
        val at = defaultReminderSendAt(LocalDateTime.of(2026, 11, 11, 10, 1, 30))
        assertEquals("2026-11-11T10:10", at)
        val page = contextaJson.decodeFromString<SchedulePage>(
            """{"data":[{"id":"s1","title":"Diária","cron":"0 9 * * 1-5","enabled":true}]}""",
        )
        assertTrue(page.data.single().enabled)
        assertEquals("Agendado", reminderStatusLabel("pending"))
    }

    @Test
    fun companyAndMemoryParse() {
        val company = contextaJson.decodeFromString<Company>(
            """{"id":"e1","name":"ACME","notes":"cliente","open_task_count":2,"contact_count":4}""",
        )
        assertEquals("ACME", company.name)
        assertEquals(2, company.openTaskCount)
        val memory = contextaJson.decodeFromString<Memory>(
            """{"id":"n1","content":"Preferência por e-mail","source":"note","company_name":"ACME"}""",
        )
        assertEquals("Preferência por e-mail", memory.headline)
    }

    @Test
    fun dashboardWhatsappAndStale() {
        val dash = contextaJson.decodeFromString<Dashboard>(
            """{"contacts":3,"messages":10,"messages_inbound":7,"uazapi_status":"open","top_conversations":[{"id":"c1","title":"Ana","message_count":4}]}""",
        )
        assertTrue(whatsappOnline(dash.uazapiStatus))
        assertFalse(whatsappOnline("close"))
        assertEquals("Ana", dash.topConversations.single().title)
        val stale = contextaJson.decodeFromString<StalePage>(
            """{"days":30,"data":[{"id":"c2","title":"Velha","message_count":9,"inactive_days":40,"blocked":true}]}""",
        )
        assertTrue(stale.data.single().blocked)
        assertEquals("Velha", stale.data.single().displayTitle)
    }

    @Test
    fun audioWithoutTranscriptShowsStatus() {
        val pending = contextaJson.decodeFromString<Message>(
            """{"id":"m2","type":"audio","transcription":{"status":"pending"}}""",
        )
        assertEquals("[Áudio]", pending.displayText())
        assertEquals("Transcrição pendente", pending.audioLabel())
        val text = contextaJson.decodeFromString<Message>("""{"id":"m3","type":"text","text":"oi","status":"sent"}""")
        assertEquals(null, text.audioLabel())
        assertEquals("sent", text.status)
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
        assertTrue(nearLatestMessages(0))
        assertTrue(nearLatestMessages(2))
        assertFalse(nearLatestMessages(3))
        assertFalse(showStickyDay("Hoje", nearLatest = true))
        assertTrue(showStickyDay("Hoje", nearLatest = false))
        assertTrue(showStickyDay("Ontem", nearLatest = true))
        assertFalse(showStickyDay("", nearLatest = false))
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
