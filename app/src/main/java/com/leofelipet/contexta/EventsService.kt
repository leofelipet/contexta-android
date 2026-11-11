package com.leofelipet.contexta

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import okhttp3.sse.EventSource

class EventsService : Service() {
    private val api = Api()
    private val handler = Handler(Looper.getMainLooper())
    private var source: EventSource? = null
    private var stopped = false
    private var generation = 0

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Notify.ensureChannels(this)
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(Notify.LIVE_ID, Notify.connected(this), ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(Notify.LIVE_ID, Notify.connected(this))
        }
        connect()
        schedulePending()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    override fun onDestroy() {
        stopped = true
        handler.removeCallbacksAndMessages(null)
        source?.cancel()
        super.onDestroy()
    }

    private fun connect() {
        if (stopped) return
        val prefs = Prefs(this)
        if (!prefs.configured()) {
            stopSelf()
            return
        }
        source?.cancel()
        val mine = ++generation
        source = api.events(
            prefs.baseUrl,
            prefs.token,
            onFailure = {
                if (!stopped && mine == generation) handler.postDelayed({ connect() }, 4000)
            },
        ) {
            if (!AppState.inForeground()) {
                Notify.alert(this, "Nova mensagem", "Há novidade no Contexta")
            }
        }
    }

    private fun schedulePending() {
        val prefs = Prefs(this)
        if (!prefs.configured()) return
        Thread {
            try {
                val page = api.reminders(prefs.baseUrl, prefs.token, status = "pending")
                page.data.forEach { reminder ->
                    Alarms.schedule(
                        this,
                        reminder.id,
                        reminder.sendAt,
                        reminder.title.ifBlank { "Lembrete" },
                        reminder.message,
                    )
                }
            } catch (_: Exception) {
            }
        }.start()
    }
}

fun Context.startEvents() {
    if (!Prefs(this).configured()) return
    val intent = Intent(this, EventsService::class.java)
    if (Build.VERSION.SDK_INT >= 26) startForegroundService(intent) else startService(intent)
}

fun Context.stopEvents() {
    stopService(Intent(this, EventsService::class.java))
}
