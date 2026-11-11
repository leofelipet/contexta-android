package com.leofelipet.contexta

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent

object Alarms {
    fun schedule(context: Context, id: String, sendAt: String, title: String, message: String) {
        if (!Prefs(context).notifyReminders) return
        val at = reminderTriggerMillis(sendAt) ?: return
        if (at <= System.currentTimeMillis()) return
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pending(context, id, title, message))
    }

    fun cancel(context: Context, id: String) {
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        manager.cancel(pending(context, id, "", ""))
    }

    private fun pending(context: Context, id: String, title: String, message: String): PendingIntent {
        val intent = Intent(context, ReminderReceiver::class.java)
            .setAction("com.leofelipet.contexta.REMINDER")
            .putExtra("id", id)
            .putExtra("title", title)
            .putExtra("message", message)
        return PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }
}
