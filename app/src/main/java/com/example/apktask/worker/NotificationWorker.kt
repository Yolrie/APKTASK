package com.example.apktask.worker

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.apktask.R
import com.example.apktask.util.NotificationHelper

/**
 * Worker qui affiche un rappel quotidien (matin ou soir).
 *
 * Remplace la planification via [android.app.AlarmManager.setExactAndAllowWhileIdle] :
 *  - Pas de permission SCHEDULE_EXACT_ALARM (rappels non critiques à la seconde)
 *  - Survie aux redémarrages sans [com.example.apktask.receiver.BootReceiver]
 *  - WorkManager reschédule automatiquement après reboot ou Doze
 *
 * Sécurité :
 *  - Vérifie POST_NOTIFICATIONS avant tout affichage (Android 13+)
 *  - Aucune donnée utilisateur dans le titre ou le corps de la notification
 *  - Le type ("morning"/"evening") est la seule donnée transmise via [androidx.work.Data]
 *  - En cas de permission manquante : Result.success() (pas d'erreur — l'utilisateur a refusé)
 */
class NotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        const val KEY_TYPE = "notif_type"
        const val TYPE_MORNING = "morning"
        const val TYPE_EVENING = "evening"

        private const val NOTIF_ID_MORNING = 3001
        private const val NOTIF_ID_EVENING = 3002
    }

    override suspend fun doWork(): Result {
        // Permission requise depuis Android 13 (API 33 — TIRAMISU)
        if (ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // L'utilisateur n'a pas accordé la permission : succès silencieux
            return Result.success()
        }

        val type = inputData.getString(KEY_TYPE) ?: return Result.failure()

        data class NotifContent(val title: String, val body: String, val id: Int)

        val content = when (type) {
            TYPE_MORNING -> NotifContent(
                title = "Bonne journée \uD83C\uDF1F",
                body  = "Planifiez vos tâches du jour.",
                id    = NOTIF_ID_MORNING
            )
            TYPE_EVENING -> NotifContent(
                title = "Bilan du jour \u2705",
                body  = "Vérifiez vos tâches complétées.",
                id    = NOTIF_ID_EVENING
            )
            else -> return Result.failure()
        }

        val notification = NotificationCompat.Builder(applicationContext, NotificationHelper.CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_do_it)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
            .notify(content.id, notification)

        return Result.success()
    }
}
