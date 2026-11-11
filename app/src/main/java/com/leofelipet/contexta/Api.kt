package com.leofelipet.contexta

import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import okhttp3.sse.EventSources
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

class Unauthorized : IOException("unauthorized")
class ApiException(val code: Int) : IOException("http $code")

class Api(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS)
        .build(),
) {
    fun conversations(
        base: String,
        token: String,
        cursor: String? = null,
        query: String = "",
        type: String = "",
    ): ConversationPage {
        val url = apiUrl(base, "conversations").newBuilder()
            .addQueryParameter("limit", "20")
            .apply {
                if (!cursor.isNullOrBlank()) addQueryParameter("cursor", cursor)
                if (query.isNotBlank()) addQueryParameter("query", query)
                if (type.isNotBlank()) addQueryParameter("type", type)
            }
            .build()
        return contextaJson.decodeFromString(get(url.toString(), token))
    }

    fun denylist(base: String, token: String): DenylistPage {
        // shortcut: first 200, paginate if a denylist grows past that
        val url = apiUrl(base, "denylist").newBuilder()
            .addQueryParameter("limit", "200")
            .build()
        return contextaJson.decodeFromString(get(url.toString(), token))
    }

    fun messages(base: String, token: String, conversationId: String, cursor: String? = null): MessagePage {
        val url = apiUrl(base, "conversations/$conversationId/messages").newBuilder()
            .addQueryParameter("limit", "20")
            .apply { if (!cursor.isNullOrBlank()) addQueryParameter("cursor", cursor) }
            .build()
        return contextaJson.decodeFromString(get(url.toString(), token))
    }

    fun githubLatest(): GithubRelease? {
        val request = Request.Builder()
            .url("https://api.github.com/repos/leofelipet/contexta-android/releases/latest")
            .header("Accept", "application/vnd.github+json")
            .header("User-Agent", "Contexta-Android")
            .build()
        client.newCall(request).execute().use { response ->
            if (response.code == 404) return null
            if (!response.isSuccessful) throw ApiException(response.code)
            return contextaJson.decodeFromString(response.body?.string().orEmpty())
        }
    }

    fun downloadUrl(url: String, dest: File) {
        dest.parentFile?.mkdirs()
        val request = Request.Builder().url(url).header("User-Agent", "Contexta-Android").build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw ApiException(response.code)
            val body = response.body ?: throw ApiException(response.code)
            dest.outputStream().use { body.byteStream().copyTo(it) }
        }
    }

    fun events(base: String, token: String, onMessages: () -> Unit): EventSource {
        val request = authed(apiUrl(base, "events").toString(), token)
            .newBuilder()
            .header("Accept", "text/event-stream")
            .build()
        return EventSources.createFactory(client).newEventSource(request, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (type == "messages") onMessages()
            }
        })
    }

    private fun get(url: String, token: String): String {
        client.newCall(authed(url, token)).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (response.code == 401) throw Unauthorized()
            if (!response.isSuccessful) throw ApiException(response.code)
            return body
        }
    }

    private fun authed(url: String, token: String): Request =
        Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $token")
            .build()

    private fun apiUrl(base: String, path: String) =
        normalizeBaseUrl(base).toHttpUrl().newBuilder().addPathSegments("api/v1/$path").build()
}
