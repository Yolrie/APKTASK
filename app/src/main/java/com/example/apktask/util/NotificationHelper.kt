package com.example.apktask.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.apktask.receiver.NotificationReceiver
import java.util.Calendar

/**
 * Gestion des canaux de notification et planification des rappels quotidiens.
 *
 * Sécurité :
 *  - Aucune donnée utilisateur dans le corps des notifications
 *  - PendingIntent.FLAG_IMMUTABLE requis depuis Android 12
 *  - Vérification canScheduleExactAlarms() avant toute planification exacte
 *  - Les alarmes sont annulables depuis le profil utilisateur
 */
object NotificationHelper {

    const val CHANNEL_ID = "do_it_rappels"
    const val EXTRA_TYPE = "notif_type"
    const val TYPE_MORNING = "morning"
    const val TYPE_EVENING = "evening"

    private const val REQ_MORNING = 2001
    private const val REQ_EVENING = 2002

    fun createChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Do.it — Rappels journaliers",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Rappels pour démarrer et vérifier vos tâches du jour"
            enableVibration(true)
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .createNotificationChannel(channel)
    }

    fun scheduleMorning(context: Context, hourOfDay: Int) {
        schedule(context, hourOfDay, REQ_MORNING, TYPE_MORNING)
    }

    fun scheduleEvening(context: Context, hourOfDay: Int) {
        schedule(context, hourOfDay, REQ_EVENING, TYPE_EVENING)
    }

    fun cancelAll(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(buildIntent(context, REQ_MORNING, TYPE_MORNING))
        am.cancel(buildIntent(context, REQ_EVENING, TYPE_EVENING))
    }

    private fun schedule(context: Context, hour: Int, reqCode: Int, type: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // Sur Android 12+, l'autorisation exacte doit être accordée par l'utilisateur
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !am.canScheduleExactAlarms()) return

        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }.timeInMillis

        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, buildIntent(context, reqCode, type))
    }

    private fun buildIntent(context: Context, reqCode: Int, type: String): PendingIntent {
        val intent = Intent(context, NotificationReceiver::class.java).putExtra(EXTRA_TYPE, type)
        return PendingIntent.getBroadcast(
            context, reqCode, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
