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
    var contactQuery: String = ""
    var contacts: List<Contact> = emptyList()
    var contactCursor: String? = null
    var contactsReady: Boolean = false
    val contactListState = LazyListState()
    var searchQuery: String = ""
    var searchDirection: String = ""
    var searchType: String = ""
    var searchResults: List<Message> = emptyList()
    var searchCursor: String? = null
    var searchReady: Boolean = false
    val searchListState = LazyListState()
    var taskQuery: String = ""
    var taskFilter: String = ""
    var tasks: List<Task> = emptyList()
    var taskCursor: String? = null
    var tasksReady: Boolean = false
    val taskListState = LazyListState()
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
        contactQuery = ""
        contacts = emptyList()
        contactCursor = null
        contactsReady = false
        searchQuery = ""
        searchDirection = ""
        searchType = ""
        searchResults = emptyList()
        searchCursor = null
        searchReady = false
        taskQuery = ""
        taskFilter = ""
        tasks = emptyList()
        taskCursor = null
        tasksReady = false
        threads.clear()
    }

    fun invalidateTasks() {
        tasks = emptyList()
        taskCursor = null
        tasksReady = false
    }
}

class ThreadCache(
    var items: List<Message> = emptyList(),
    var cursor: String? = null,
    var detail: Conversation? = null,
    val listState: LazyListState = LazyListState(),
)
