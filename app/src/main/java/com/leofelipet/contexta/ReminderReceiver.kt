package com.leofelipet.contexta

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (!Prefs(context).notifyReminders) return
        val title = intent.getStringExtra("title").orEmpty().ifBlank { "Lembrete" }
        val message = intent.getStringExtra("message").orEmpty().ifBlank { "Horário do lembrete." }
        Notify.alert(context, title, message)
    }
}
