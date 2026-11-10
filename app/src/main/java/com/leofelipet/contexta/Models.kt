package com.leofelipet.contexta

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

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
    @SerialName("last_message_at") val lastMessageAt: String? = null,
    @SerialName("last_message") val lastMessage: Preview? = null,
) {
    val displayTitle: String get() = title.ifBlank { "Conversa" }
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
    val timestamp: String = "",
    val transcription: Transcription? = null,
)

@Serializable
data class Transcription(
    val status: String = "",
    val text: String? = null,
)

@Serializable
data class AndroidUpdate(
    @SerialName("version_code") val versionCode: Int,
    @SerialName("version_name") val versionName: String = "",
    val notes: String = "",
)

fun needsUpdate(localVersionCode: Int, remoteVersionCode: Int) = remoteVersionCode > localVersionCode

fun Message.displayText(): String {
    val transcript = transcription?.text?.trim().orEmpty()
    if (transcript.isNotEmpty()) return transcript
    val body = text?.trim().orEmpty()
    if (body.isNotEmpty()) return body
    return if (type.equals("audio", ignoreCase = true)) "[Áudio]" else "[${type.ifBlank { "mensagem" }}]"
}

fun Preview.displayText(): String {
    val body = text.trim()
    if (body.isNotEmpty()) return body
    return if (type.equals("audio", ignoreCase = true)) "[Áudio]" else type
}

fun formatTimestamp(iso: String): String {
    if (iso.length < 16) return iso
    return iso.substring(8, 10) + "/" + iso.substring(5, 7) + " " + iso.substring(11, 16)
}

fun normalizeBaseUrl(raw: String): String {
    var value = raw.trim()
    if (!value.startsWith("http://") && !value.startsWith("https://")) {
        value = "https://$value"
    }
    value = value.trimEnd('/')
    if (value.endsWith("/api/v1")) value = value.removeSuffix("/api/v1")
    return value.trimEnd('/')
}
