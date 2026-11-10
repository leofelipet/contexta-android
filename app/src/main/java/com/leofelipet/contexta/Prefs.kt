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

    fun clear() {
        prefs.edit().clear().apply()
    }
}
