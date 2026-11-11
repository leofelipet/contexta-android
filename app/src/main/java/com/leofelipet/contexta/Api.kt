package com.leofelipet.contexta

import kotlinx.serialization.encodeToString
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
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
        contactId: String = "",
        companyId: String = "",
    ): ConversationPage {
        val url = apiUrl(base, "conversations").newBuilder()
            .addQueryParameter("limit", "20")
            .apply {
                if (!cursor.isNullOrBlank()) addQueryParameter("cursor", cursor)
                if (query.isNotBlank()) addQueryParameter("query", query)
                if (type.isNotBlank()) addQueryParameter("type", type)
                if (contactId.isNotBlank()) addQueryParameter("contact_id", contactId)
                if (companyId.isNotBlank()) addQueryParameter("company_id", companyId)
            }
            .build()
        return contextaJson.decodeFromString(get(url.toString(), token))
    }

    fun contacts(
        base: String,
        token: String,
        cursor: String? = null,
        query: String = "",
        companyId: String = "",
    ): ContactPage {
        val url = apiUrl(base, "contacts").newBuilder()
            .addQueryParameter("limit", "20")
            .apply {
                if (!cursor.isNullOrBlank()) addQueryParameter("cursor", cursor)
                if (query.isNotBlank()) addQueryParameter("query", query)
                if (companyId.isNotBlank()) addQueryParameter("company_id", companyId)
            }
            .build()
        return contextaJson.decodeFromString(get(url.toString(), token))
    }

    fun searchMessages(
        base: String,
        token: String,
        cursor: String? = null,
        query: String = "",
        direction: String = "",
        type: String = "",
    ): MessagePage {
        val url = apiUrl(base, "messages").newBuilder()
            .addQueryParameter("limit", "20")
            .apply {
                if (!cursor.isNullOrBlank()) addQueryParameter("cursor", cursor)
                if (query.isNotBlank()) addQueryParameter("query", query)
                if (direction.isNotBlank()) addQueryParameter("direction", direction)
                if (type.isNotBlank()) addQueryParameter("type", type)
            }
            .build()
        return contextaJson.decodeFromString(get(url.toString(), token))
    }

    fun denylist(base: String, token: String, cursor: String? = null, limit: Int = 200): DenylistPage {
        val url = apiUrl(base, "denylist").newBuilder()
            .addQueryParameter("limit", limit.toString())
            .apply { if (!cursor.isNullOrBlank()) addQueryParameter("cursor", cursor) }
            .build()
        return contextaJson.decodeFromString(get(url.toString(), token))
    }

    fun removeDenylist(base: String, token: String, id: String) {
        delete(apiUrl(base, "denylist/$id").toString(), token)
    }

    fun dashboard(base: String, token: String): Dashboard =
        contextaJson.decodeFromString(get(apiUrl(base, "dashboard").toString(), token))

    fun system(base: String, token: String): SystemOverview =
        contextaJson.decodeFromString(get(apiUrl(base, "system").toString(), token))

    fun activity(
        base: String,
        token: String,
        cursor: String? = null,
        category: String = "",
        level: String = "",
    ): ActivityPage {
        val url = apiUrl(base, "activity").newBuilder()
            .addQueryParameter("limit", "20")
            .apply {
                if (!cursor.isNullOrBlank()) addQueryParameter("cursor", cursor)
                if (category.isNotBlank()) addQueryParameter("category", category)
                if (level.isNotBlank()) addQueryParameter("level", level)
            }
            .build()
        return contextaJson.decodeFromString(get(url.toString(), token))
    }

    fun staleConversations(base: String, token: String, days: Int = 30, cursor: String? = null): StalePage {
        val url = apiUrl(base, "conversations/stale").newBuilder()
            .addQueryParameter("days", days.toString())
            .addQueryParameter("limit", "20")
            .apply { if (!cursor.isNullOrBlank()) addQueryParameter("cursor", cursor) }
            .build()
        return contextaJson.decodeFromString(get(url.toString(), token))
    }

    fun deleteConversation(base: String, token: String, id: String) {
        delete(apiUrl(base, "conversations/$id").toString(), token)
    }

    fun messages(base: String, token: String, conversationId: String, cursor: String? = null): MessagePage {
        val url = apiUrl(base, "conversations/$conversationId/messages").newBuilder()
            .addQueryParameter("limit", "20")
            .apply { if (!cursor.isNullOrBlank()) addQueryParameter("cursor", cursor) }
            .build()
        return contextaJson.decodeFromString(get(url.toString(), token))
    }

    fun conversation(base: String, token: String, id: String): Conversation =
        contextaJson.decodeFromString(get(apiUrl(base, "conversations/$id").toString(), token))

    fun companies(
        base: String,
        token: String,
        cursor: String? = null,
        query: String = "",
        limit: Int = 200,
    ): CompanyPage {
        val url = apiUrl(base, "companies").newBuilder()
            .addQueryParameter("limit", limit.toString())
            .apply {
                if (!cursor.isNullOrBlank()) addQueryParameter("cursor", cursor)
                if (query.isNotBlank()) addQueryParameter("q", query)
            }
            .build()
        return contextaJson.decodeFromString(get(url.toString(), token))
    }

    fun company(base: String, token: String, id: String): Company =
        contextaJson.decodeFromString(get(apiUrl(base, "companies/$id").toString(), token))

    fun createCompany(base: String, token: String, body: CreateCompanyBody): Company =
        contextaJson.decodeFromString(post(apiUrl(base, "companies").toString(), token, contextaJson.encodeToString(body)))

    fun updateCompany(base: String, token: String, id: String, body: UpdateCompanyBody): Company =
        contextaJson.decodeFromString(patch(apiUrl(base, "companies/$id").toString(), token, contextaJson.encodeToString(body)))

    fun deleteCompany(base: String, token: String, id: String) {
        delete(apiUrl(base, "companies/$id").toString(), token)
    }

    fun contact(base: String, token: String, id: String): Contact =
        contextaJson.decodeFromString(get(apiUrl(base, "contacts/$id").toString(), token))

    fun tasks(
        base: String,
        token: String,
        cursor: String? = null,
        query: String = "",
        openOnly: Boolean = false,
        overdue: Boolean = false,
        companyId: String = "",
    ): TaskPage {
        val url = apiUrl(base, "tasks").newBuilder()
            .addQueryParameter("limit", "20")
            .apply {
                if (!cursor.isNullOrBlank()) addQueryParameter("cursor", cursor)
                if (query.isNotBlank()) addQueryParameter("q", query)
                if (openOnly) addQueryParameter("open_only", "1")
                if (overdue) addQueryParameter("overdue", "1")
                if (companyId.isNotBlank()) addQueryParameter("company_id", companyId)
            }
            .build()
        return contextaJson.decodeFromString(get(url.toString(), token))
    }

    fun task(base: String, token: String, id: String): Task =
        contextaJson.decodeFromString(get(apiUrl(base, "tasks/$id").toString(), token))

    fun createTask(base: String, token: String, body: CreateTaskBody): Task =
        contextaJson.decodeFromString(post(apiUrl(base, "tasks").toString(), token, contextaJson.encodeToString(body)))

    fun updateTask(base: String, token: String, id: String, body: UpdateTaskBody): Task =
        contextaJson.decodeFromString(patch(apiUrl(base, "tasks/$id").toString(), token, contextaJson.encodeToString(body)))

    fun deleteTask(base: String, token: String, id: String) {
        delete(apiUrl(base, "tasks/$id").toString(), token)
    }

    fun schedules(base: String, token: String, cursor: String? = null, query: String = ""): SchedulePage {
        val url = apiUrl(base, "task-schedules").newBuilder()
            .addQueryParameter("limit", "20")
            .apply {
                if (!cursor.isNullOrBlank()) addQueryParameter("cursor", cursor)
                if (query.isNotBlank()) addQueryParameter("q", query)
            }
            .build()
        return contextaJson.decodeFromString(get(url.toString(), token))
    }

    fun setScheduleEnabled(base: String, token: String, id: String, enabled: Boolean) {
        patch(apiUrl(base, "task-schedules/$id").toString(), token, contextaJson.encodeToString(UpdateScheduleBody(enabled)))
    }

    fun deleteSchedule(base: String, token: String, id: String) {
        delete(apiUrl(base, "task-schedules/$id").toString(), token)
    }

    fun reminders(base: String, token: String, cursor: String? = null, status: String = ""): ReminderPage {
        val url = apiUrl(base, "reminders").newBuilder()
            .addQueryParameter("limit", "20")
            .apply {
                if (!cursor.isNullOrBlank()) addQueryParameter("cursor", cursor)
                if (status.isNotBlank()) addQueryParameter("status", status)
            }
            .build()
        return contextaJson.decodeFromString(get(url.toString(), token))
    }

    fun createReminder(base: String, token: String, body: CreateReminderBody): Reminder =
        contextaJson.decodeFromString(post(apiUrl(base, "reminders").toString(), token, contextaJson.encodeToString(body)))

    fun deleteReminder(base: String, token: String, id: String) {
        delete(apiUrl(base, "reminders/$id").toString(), token)
    }

    fun memories(base: String, token: String, cursor: String? = null, query: String = ""): MemoryPage {
        val url = apiUrl(base, "memories").newBuilder()
            .addQueryParameter("limit", "20")
            .apply {
                if (!cursor.isNullOrBlank()) addQueryParameter("cursor", cursor)
                if (query.isNotBlank()) addQueryParameter("q", query)
            }
            .build()
        return contextaJson.decodeFromString(get(url.toString(), token))
    }

    fun memory(base: String, token: String, id: String): Memory =
        contextaJson.decodeFromString(get(apiUrl(base, "memories/$id").toString(), token))

    fun createMemory(base: String, token: String, body: CreateMemoryBody): Memory =
        contextaJson.decodeFromString(post(apiUrl(base, "memories").toString(), token, contextaJson.encodeToString(body)))

    fun deleteMemory(base: String, token: String, id: String) {
        delete(apiUrl(base, "memories/$id").toString(), token)
    }

    fun uazapi(base: String, token: String): UazapiIntegration =
        contextaJson.decodeFromString(get(apiUrl(base, "integrations/uazapi").toString(), token))

    fun mcpStatus(base: String, token: String): McpStatus =
        contextaJson.decodeFromString(get(apiUrl(base, "mcp/status").toString(), token))

    fun emailAccounts(base: String, token: String): EmailAccountPage {
        val url = apiUrl(base, "email-accounts").newBuilder()
            .addQueryParameter("limit", "50")
            .build()
        return contextaJson.decodeFromString(get(url.toString(), token))
    }

    fun testEmailAccount(base: String, token: String, id: String): EmailTestResult {
        val request = authed(apiUrl(base, "email-accounts/$id/test").toString(), token)
            .newBuilder()
            .post("{}".toRequestBody(jsonMedia))
            .build()
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (response.code == 401) throw Unauthorized()
            if (!response.isSuccessful && response.code != 502) throw ApiException(response.code)
            return contextaJson.decodeFromString(body)
        }
    }

    fun addDenylist(base: String, token: String, targetType: String, targetId: String, reason: String) {
        post(
            apiUrl(base, "denylist").toString(),
            token,
            contextaJson.encodeToString(AddDenylistBody(targetType, targetId, reason)),
        )
    }

    fun attachConversation(base: String, token: String, companyId: String, conversationId: String) {
        post(
            apiUrl(base, "companies/$companyId/conversations").toString(),
            token,
            contextaJson.encodeToString(AttachConversationBody(conversationId)),
        )
    }

    fun detachConversation(base: String, token: String, companyId: String, conversationId: String) {
        delete(apiUrl(base, "companies/$companyId/conversations/$conversationId").toString(), token)
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

    fun events(
        base: String,
        token: String,
        onFailure: () -> Unit = {},
        onMessages: () -> Unit,
    ): EventSource {
        val request = authed(apiUrl(base, "events").toString(), token)
            .newBuilder()
            .header("Accept", "text/event-stream")
            .build()
        return EventSources.createFactory(client).newEventSource(request, object : EventSourceListener() {
            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                if (type == "messages") onMessages()
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: okhttp3.Response?) {
                onFailure()
            }
        })
    }

    private fun get(url: String, token: String): String = execute(authed(url, token))

    private fun post(url: String, token: String, json: String): String =
        execute(authed(url, token).newBuilder().post(json.toRequestBody(jsonMedia)).build())

    private fun patch(url: String, token: String, json: String): String =
        execute(authed(url, token).newBuilder().patch(json.toRequestBody(jsonMedia)).build())

    private fun delete(url: String, token: String): String =
        execute(authed(url, token).newBuilder().delete().build())

    private fun execute(request: Request): String {
        client.newCall(request).execute().use { response ->
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

    private companion object {
        val jsonMedia = "application/json; charset=utf-8".toMediaType()
    }

    private fun apiUrl(base: String, path: String) =
        normalizeBaseUrl(base).toHttpUrl().newBuilder().addPathSegments("api/v1/$path").build()
}
