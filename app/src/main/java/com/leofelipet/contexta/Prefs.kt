package com.leofelipet.contexta

import android.content.Context

class Prefs(context: Context) {
    private val prefs = context.getSharedPreferences("contexta", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString("base_url", "") ?: ""
        set(value) { prefs.edit().putString("base_url", value).apply() }

    var token: String
        get() = prefs.getString("token", "") ?: ""
        set(value) { prefs.edit().putString("token", value).apply() }

    fun configured() = baseUrl.isNotBlank() && token.isNotBlank()

    var notifyMessages: Boolean
        get() = prefs.getBoolean("notify_messages", false)
        set(value) { prefs.edit().putBoolean("notify_messages", value).apply() }

    var notifyReminders: Boolean
        get() = prefs.getBoolean("notify_reminders", true)
        set(value) { prefs.edit().putBoolean("notify_reminders", value).apply() }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
