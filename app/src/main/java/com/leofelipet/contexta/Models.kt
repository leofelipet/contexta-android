package com.leofelipet.contexta

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val contextaJson = Json { ignoreUnknownKeys = true; explicitNulls = false }

@Serializable
data class ConversationPage(
    val data: List<Conversation> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
)

@Serializable
data class MessagePage(
    val data: List<Message> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
)

@Serializable
data class Conversation(
    val id: String,
    val title: String = "",
    val type: String = "",
    @SerialName("contact_id") val contactId: String = "",
    @SerialName("company_id") val companyId: String = "",
    @SerialName("company_name") val companyName: String = "",
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("last_message") val lastMessage: Preview? = null,
) {
    val displayTitle: String get() = title.ifBlank { "Conversa" }
    val isGroup: Boolean get() = type.equals("group", ignoreCase = true)
}

@Serializable
data class CompanyPage(
    val data: List<Company> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
)

@Serializable
data class Company(
    val id: String,
    val name: String = "",
)

@Serializable
data class Contact(
    val id: String,
    val name: String = "",
    val phone: String = "",
    @SerialName("push_name") val pushName: String = "",
    @SerialName("company_name") val companyName: String = "",
) {
    val displayName: String
        get() {
            val named = name.trim().ifBlank { pushName.trim() }
            if (named.isNotBlank() && named != phone) return named
            return if (phone.isNotBlank()) "Contato sem nome" else "Contato"
        }
}

@Serializable
data class AttachConversationBody(@SerialName("conversation_id") val conversationId: String)

@Serializable
data class AddDenylistBody(
    @SerialName("target_type") val targetType: String,
    @SerialName("target_id") val targetId: String,
    val reason: String = "",
)

@Serializable
data class DenylistPage(
    val data: List<DenylistEntry> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
)

@Serializable
data class DenylistEntry(
    val id: String,
    @SerialName("target_type") val targetType: String = "",
    @SerialName("target_id") val targetId: String = "",
)

fun Conversation.isBlocked(conversationIds: Set<String>, contactIds: Set<String>): Boolean {
    if (id in conversationIds) return true
    return contactId.isNotBlank() && contactId in contactIds
}

fun denylistSets(entries: List<DenylistEntry>): Pair<Set<String>, Set<String>> {
    val conversations = mutableSetOf<String>()
    val contacts = mutableSetOf<String>()
    for (entry in entries) {
        when (entry.targetType) {
            "conversation" -> conversations.add(entry.targetId)
            "contact" -> contacts.add(entry.targetId)
        }
    }
    return conversations to contacts
}

@Serializable
data class Preview(
    val text: String = "",
    val type: String = "",
    val timestamp: String = "",
)

@Serializable
data class Message(
    val id: String,
    val sender: String? = null,
    val direction: String = "",
    val type: String = "",
    val text: String? = null,
    val status: String? = null,
    val timestamp: String = "",
    val transcription: Transcription? = null,
) {
    val isAudio: Boolean get() = type.equals("audio", ignoreCase = true)
}

@Serializable
data class Transcription(
    val status: String = "",
    val text: String? = null,
)

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String = "",
    val body: String = "",
    val assets: List<GithubAsset> = emptyList(),
) {
    val versionName: String get() = tagName.trim().trimStart('v', 'V')
    val apkUrl: String? get() = assets.firstOrNull { it.name.endsWith(".apk", ignoreCase = true) }?.url
}

@Serializable
data class GithubAsset(
    val name: String = "",
    @SerialName("browser_download_url") val url: String = "",
)

fun needsUpdate(localVersionName: String, remoteVersionName: String): Boolean {
    fun parts(value: String) = value.trim().trimStart('v', 'V').split('.').map { it.toIntOrNull() ?: 0 }
    val local = parts(localVersionName)
    val remote = parts(remoteVersionName)
    val size = maxOf(local.size, remote.size)
    for (index in 0 until size) {
        val delta = remote.getOrElse(index) { 0 } - local.getOrElse(index) { 0 }
        if (delta != 0) return delta > 0
    }
    return false
}

fun Message.displayText(): String {
    val transcript = transcription?.text?.trim().orEmpty()
    if (transcript.isNotEmpty()) return transcript
    val body = text?.trim().orEmpty()
    if (body.isNotEmpty()) return body
    return if (isAudio) "[Áudio]" else "[${type.ifBlank { "mensagem" }}]"
}

fun Message.audioLabel(): String? {
    if (!isAudio) return null
    if (!transcription?.text?.trim().isNullOrEmpty()) return "Transcrição do áudio"
    return when (transcription?.status?.lowercase()) {
        "pending" -> "Transcrição pendente"
        "processing" -> "Transcrição em processamento"
        "retry" -> "Nova tentativa de transcrição"
        "failed" -> "Não foi possível transcrever o áudio"
        else -> "Áudio"
    }
}

fun Preview.displayText(): String {
    val body = text.trim()
    if (body.isNotEmpty()) return body
    return if (type.equals("audio", ignoreCase = true)) "[Áudio]" else type
}

private val months = arrayOf(
    "janeiro", "fevereiro", "março", "abril", "maio", "junho",
    "julho", "agosto", "setembro", "outubro", "novembro", "dezembro",
)

fun parseInstant(iso: String, zone: ZoneId = ZoneId.systemDefault()): Instant? {
    if (iso.isBlank()) return null
    try {
        return Instant.parse(iso)
    } catch (_: Exception) {
    }
    try {
        return OffsetDateTime.parse(iso).toInstant()
    } catch (_: Exception) {
    }
    return try {
        LocalDateTime.parse(iso).atZone(zone).toInstant()
    } catch (_: Exception) {
        null
    }
}

fun localDateOf(iso: String, zone: ZoneId = ZoneId.systemDefault()): LocalDate? {
    val instant = parseInstant(iso, zone)
    if (instant != null) return instant.atZone(zone).toLocalDate()
    if (iso.length < 10) return null
    return try {
        LocalDate.parse(iso.take(10))
    } catch (_: Exception) {
        null
    }
}

fun dayLabel(iso: String, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): String {
    val date = localDateOf(iso, zone) ?: return ""
    return when (date) {
        today -> "Hoje"
        today.minusDays(1) -> "Ontem"
        else -> {
            val base = "${date.dayOfMonth} de ${months[date.monthValue - 1]}"
            if (date.year == today.year) base else "$base de ${date.year}"
        }
    }
}

fun sameDay(a: String, b: String, zone: ZoneId = ZoneId.systemDefault()): Boolean {
    val left = localDateOf(a, zone) ?: return false
    val right = localDateOf(b, zone) ?: return false
    return left == right
}

fun showDayChip(current: String, older: String?, zone: ZoneId = ZoneId.systemDefault()): Boolean {
    if (localDateOf(current, zone) == null) return false
    if (older == null) return true
    return !sameDay(current, older, zone)
}

fun nearLatestMessages(firstVisibleIndex: Int): Boolean = firstVisibleIndex <= 2

fun showStickyDay(label: String?, nearLatest: Boolean): Boolean {
    if (label.isNullOrBlank()) return false
    if (label == "Hoje" && nearLatest) return false
    return true
}

fun formatClock(iso: String, zone: ZoneId = ZoneId.systemDefault()): String {
    val instant = parseInstant(iso, zone) ?: return if (iso.length >= 16) iso.substring(11, 16) else iso
    return DateTimeFormatter.ofPattern("HH:mm").withZone(zone).format(instant)
}

fun formatListTime(iso: String, today: LocalDate = LocalDate.now(), zone: ZoneId = ZoneId.systemDefault()): String {
    val date = localDateOf(iso, zone) ?: return formatTimestamp(iso)
    return when (date) {
        today -> formatClock(iso, zone)
        today.minusDays(1) -> "Ontem"
        else -> date.format(DateTimeFormatter.ofPattern("dd/MM"))
    }
}

fun formatTimestamp(iso: String): String {
    if (iso.length < 16) return iso
    return iso.substring(8, 10) + "/" + iso.substring(5, 7) + " " + iso.substring(11, 16)
}

/** 15th row of a 20-row page, and the same distance from the end on the next pages. */
fun shouldPrefetch(lastVisible: Int, count: Int): Boolean {
    if (count <= 0 || lastVisible < 0) return false
    if (lastVisible < count - 6) return false
    return lastVisible >= 14 || lastVisible >= count - 1
}

fun shouldLoadOlder(maxVisible: Int, count: Int): Boolean =
    count > 0 && maxVisible >= count - 3

fun normalizeBaseUrl(raw: String): String {
    var value = raw.trim()
    if (!value.startsWith("http://") && !value.startsWith("https://")) {
        value = "https://$value"
    }
    value = value.trimEnd('/')
    if (value.endsWith("/api/v1")) value = value.removeSuffix("/api/v1")
    return value.trimEnd('/')
}
