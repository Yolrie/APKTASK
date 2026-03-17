package com.example.apktask.util

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.apktask.worker.MidnightResetWorker
import com.example.apktask.worker.NotificationWorker
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Centralise la planification de tous les Workers de l'application via WorkManager.
 *
 * Avantages vs AlarmManager :
 *  - Idempotent : plusieurs appels ne créent pas plusieurs alarmes (UniquePeriodicWork)
 *  - Persistance automatique après reboot (WorkManager enregistre son propre BootReceiver)
 *  - Pas de permission SCHEDULE_EXACT_ALARM pour des rappels à ± quelques minutes
 *  - API testable : WorkManager offre une implémentation in-memory pour les tests
 *
 * Politique de planification :
 *  - MidnightReset → KEEP : ne replanifie pas si déjà actif (stabilité)
 *  - Notifications  → REPLACE : replanifie si l'heure change dans le profil
 *
 * Sécurité :
 *  - Tags constants : annulation ciblée sans effet de bord sur d'autres Workers
 *  - Aucune donnée sensible dans les [Data] transmises (type = "morning"/"evening" seulement)
 *  - applicationContext transmis à WorkManager : pas de fuite d'Activity
 */
object WorkScheduler {

    private const val WORK_MIDNIGHT   = "midnight_reset"
    private const val WORK_NOTIF_AM   = "notif_morning"
    private const val WORK_NOTIF_PM   = "notif_evening"

    // ── API publique ──────────────────────────────────────────────────────────

    /**
     * Initialise (ou réinitialise) tous les Workers planifiés.
     * Idempotent : peut être appelé à chaque démarrage de l'application.
     *
     * @param morningHour heure du rappel du matin (défaut : 8h)
     * @param eveningHour heure du rappel du soir  (défaut : 20h)
     */
    fun init(context: Context, morningHour: Int = 8, eveningHour: Int = 20) {
        scheduleMidnightReset(context)
        scheduleNotification(context, morningHour, WORK_NOTIF_AM, NotificationWorker.TYPE_MORNING)
        scheduleNotification(context, eveningHour, WORK_NOTIF_PM, NotificationWorker.TYPE_EVENING)
    }

    /** Annule uniquement les notifications (ex : utilisateur désactive les rappels). */
    fun cancelNotifications(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(WORK_NOTIF_AM)
        wm.cancelUniqueWork(WORK_NOTIF_PM)
    }

    /** Annule l'ensemble des Workers gérés par ce scheduler. */
    fun cancelAll(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(WORK_MIDNIGHT)
        wm.cancelUniqueWork(WORK_NOTIF_AM)
        wm.cancelUniqueWork(WORK_NOTIF_PM)
    }

    // ── Planification interne ─────────────────────────────────────────────────

    private fun scheduleMidnightReset(context: Context) {
        val request = PeriodicWorkRequestBuilder<MidnightResetWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(millisUntilHour(0), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_MIDNIGHT,
            ExistingPeriodicWorkPolicy.KEEP, // Ne déplace pas l'alarme si déjà planifiée
            request
        )
    }

    private fun scheduleNotification(
        context: Context,
        hourOfDay: Int,
        workName: String,
        type: String
    ) {
        val data = Data.Builder()
            .putString(NotificationWorker.KEY_TYPE, type)
            .build()

        val request = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.DAYS)
            .setInitialDelay(millisUntilHour(hourOfDay), TimeUnit.MILLISECONDS)
            .setInputData(data)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            workName,
            ExistingPeriodicWorkPolicy.REPLACE, // Replanifie si l'heure change dans les préférences
            request
        )
    }

    /**
     * Calcule le délai en millisecondes jusqu'à la prochaine occurrence de [hour]:00.
     * Si l'heure est déjà passée aujourd'hui, renvoie le délai jusqu'à demain.
     */
    private fun millisUntilHour(hour: Int): Long {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }
        return cal.timeInMillis - now
    }
}
