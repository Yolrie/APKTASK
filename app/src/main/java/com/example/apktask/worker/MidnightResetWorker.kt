package com.example.apktask.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.apktask.data.LocalDataSource
import com.example.apktask.data.UserRepository
import com.example.apktask.util.DateUtils

/**
 * Worker exécuté à minuit pour évaluer le streak puis purger les tâches du jour écoulé.
 *
 * Remplace le déclenchement via [android.content.Intent.ACTION_DATE_CHANGED] :
 *  - Garanti même si l'appareil était éteint (WorkManager persiste après reboot)
 *  - Pas de permission RECEIVE_BOOT_COMPLETED nécessaire (WorkManager l'embarque)
 *  - Retry automatique sur erreur transitoire (disque plein, etc.)
 *
 * Sécurité :
 *  - Aucune donnée utilisateur dans les Data d'entrée/sortie
 *  - Utilise [LocalDataSource] singleton (EncryptedSharedPreferences AES-256)
 *  - Streak évalué AVANT la purge : aucune perte de données
 *  - applicationContext uniquement : pas de fuite d'Activity
 */
class MidnightResetWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = runCatching {
        val yesterday = DateUtils.yesterday()

        // 1. Évaluation du streak sur le jour écoulé avant toute suppression
        UserRepository(applicationContext).evaluateStreakForDay(yesterday)

        // 2. Suppression chiffrée des données du jour écoulé
        LocalDataSource.getInstance(applicationContext).clearDay(yesterday)

        Result.success()
    }.getOrElse {
        // Retry si erreur transitoire (I/O, déchiffrement temporairement indisponible)
        Result.retry()
    }
}
