package com.example.apktask.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

/**
 * Gestion des canaux de notification.
 *
 * La planification des rappels est désormais déléguée à [WorkScheduler]
 * (WorkManager) en remplacement d'AlarmManager :
 *  - Plus de permission SCHEDULE_EXACT_ALARM requise
 *  - Persistance automatique après reboot
 *  - Une seule instance de chaque work (UniquePeriodicWork)
 *
 * Ce helper conserve la création du canal et les constantes partagées
 * entre [WorkScheduler] et [NotificationWorker].
 *
 * Sécurité :
 *  - Aucune donnée utilisateur dans le canal ou les constantes
 *  - PendingIntent supprimé : WorkManager gère ses propres intents en interne
 */
object NotificationHelper {

    const val CHANNEL_ID = "do_it_rappels"

    /**
     * Crée le canal de notification.
     * Idempotent : Android ignore les appels redondants si le canal existe déjà.
     * À appeler au démarrage de l'application, avant tout [WorkScheduler.init].
     */
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

    // ── Délégation vers WorkScheduler ─────────────────────────────────────────
    // Les méthodes ci-dessous maintiennent la compatibilité avec ProfileViewModel
    // qui appelle NotificationHelper pour (re)planifier selon les préférences.

    fun scheduleMorning(context: Context, hourOfDay: Int) {
        WorkScheduler.init(context, morningHour = hourOfDay)
    }

    fun scheduleEvening(context: Context, hourOfDay: Int) {
        WorkScheduler.init(context, eveningHour = hourOfDay)
    }

    fun cancelAll(context: Context) {
        WorkScheduler.cancelNotifications(context)
    }
}
