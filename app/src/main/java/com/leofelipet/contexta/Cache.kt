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
    var taskSection: String = "tarefas"
    var schedules: List<Schedule> = emptyList()
    var scheduleCursor: String? = null
    var schedulesReady: Boolean = false
    val scheduleListState = LazyListState()
    var reminders: List<Reminder> = emptyList()
    var reminderCursor: String? = null
    var remindersReady: Boolean = false
    val reminderListState = LazyListState()
    var companyQuery: String = ""
    var companyList: List<Company> = emptyList()
    var companyCursor: String? = null
    var companiesReady: Boolean = false
    val companyListState = LazyListState()
    var memoryQuery: String = ""
    var memories: List<Memory> = emptyList()
    var memoryCursor: String? = null
    var memoriesReady: Boolean = false
    val memoryListState = LazyListState()
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
        taskSection = "tarefas"
        schedules = emptyList()
        scheduleCursor = null
        schedulesReady = false
        reminders = emptyList()
        reminderCursor = null
        remindersReady = false
        companyQuery = ""
        companyList = emptyList()
        companyCursor = null
        companiesReady = false
        memoryQuery = ""
        memories = emptyList()
        memoryCursor = null
        memoriesReady = false
        threads.clear()
    }

    fun invalidateTasks() {
        tasks = emptyList()
        taskCursor = null
        tasksReady = false
    }

    fun invalidateReminders() {
        reminders = emptyList()
        reminderCursor = null
        remindersReady = false
    }

    fun invalidateCompanies() {
        companyList = emptyList()
        companyCursor = null
        companiesReady = false
        companies = emptyList()
    }

    fun invalidateMemories() {
        memories = emptyList()
        memoryCursor = null
        memoriesReady = false
    }

    fun invalidateConversations() {
        conversations = emptyList()
        conversationCursor = null
        conversationsReady = false
    }
}

class ThreadCache(
    var items: List<Message> = emptyList(),
    var cursor: String? = null,
    var detail: Conversation? = null,
    val listState: LazyListState = LazyListState(),
)
