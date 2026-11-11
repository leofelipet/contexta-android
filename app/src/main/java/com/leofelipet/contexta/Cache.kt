package com.leofelipet.contexta

import androidx.compose.foundation.lazy.LazyListState

/** shortcut: memory only, dies with the process. Disk if a cold start still flashes. */
class SessionCache {
    var conversationQuery: String = ""
    var conversationType: String = ""
    var conversations: List<Conversation> = emptyList()
    var conversationCursor: String? = null
    var conversationsReady: Boolean = false
    var blockedConversationIds: Set<String> = emptySet()
    var blockedContactIds: Set<String> = emptySet()
    var companies: List<Company> = emptyList()
    val conversationListState = LazyListState()
    val threads = mutableMapOf<String, ThreadCache>()

    fun clear() {
        conversationQuery = ""
        conversationType = ""
        conversations = emptyList()
        conversationCursor = null
        conversationsReady = false
        blockedConversationIds = emptySet()
        blockedContactIds = emptySet()
        companies = emptyList()
        threads.clear()
    }
}

class ThreadCache(
    var items: List<Message> = emptyList(),
    var cursor: String? = null,
    var detail: Conversation? = null,
    val listState: LazyListState = LazyListState(),
)
