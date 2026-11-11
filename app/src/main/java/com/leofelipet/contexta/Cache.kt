package com.leofelipet.contexta

import androidx.compose.foundation.lazy.LazyListState

/** shortcut: memory only, dies with the process. Disk if a cold start still flashes. */
class SessionCache {
    var conversationQuery: String = ""
    var conversations: List<Conversation> = emptyList()
    var conversationCursor: String? = null
    var conversationsReady: Boolean = false
    val conversationListState = LazyListState()
    val threads = mutableMapOf<String, ThreadCache>()

    fun clear() {
        conversationQuery = ""
        conversations = emptyList()
        conversationCursor = null
        conversationsReady = false
        threads.clear()
    }
}

class ThreadCache(
    var items: List<Message> = emptyList(),
    var cursor: String? = null,
    val listState: LazyListState = LazyListState(),
)
