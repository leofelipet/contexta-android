package com.leofelipet.contexta

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.concurrent.atomic.AtomicInteger

object AppState {
    private val depth = AtomicInteger(0)
    fun enter() { depth.incrementAndGet() }
    fun leave() { depth.updateAndGet { value -> if (value > 0) value - 1 else 0 } }
    fun inForeground() = depth.get() > 0
}

object Notify {
    const val CHANNEL_LIVE = "live"
    const val CHANNEL_ALERTS = "alerts"
    const val LIVE_ID = 1

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_LIVE, "Conexão", NotificationManager.IMPORTANCE_LOW),
        )
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ALERTS, "Avisos", NotificationManager.IMPORTANCE_DEFAULT),
        )
    }

    fun connected(context: Context) = NotificationCompat.Builder(context, CHANNEL_LIVE)
        .setSmallIcon(R.drawable.ic_launcher)
        .setContentTitle("Contexta")
        .setContentText("Conectado. Avisos chegam aqui.")
        .setContentIntent(openApp(context))
        .setOngoing(true)
        .setSilent(true)
        .build()

    fun alert(context: Context, title: String, text: String, id: Int = title.hashCode()) {
        ensureChannels(context)
        try {
            NotificationManagerCompat.from(context).notify(
                id,
                NotificationCompat.Builder(context, CHANNEL_ALERTS)
                    .setSmallIcon(R.drawable.ic_launcher)
                    .setContentTitle(title.ifBlank { "Contexta" })
                    .setContentText(text)
                    .setContentIntent(openApp(context))
                    .setAutoCancel(true)
                    .build(),
            )
        } catch (_: SecurityException) {
        }
    }

    private fun openApp(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
}
