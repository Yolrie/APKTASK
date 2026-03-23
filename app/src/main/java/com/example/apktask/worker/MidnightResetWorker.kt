package com.example.apktask.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.apktask.data.LocalDataSource
import com.example.apktask.data.TaskRepository
import com.example.apktask.data.UserRepository
import com.example.apktask.util.DateUtils

/**
 * Worker exécuté à minuit pour :
 *  1. Charger les tâches du jour écoulé (encore présentes en base)
 *  2. Évaluer le streak avec ces tâches
 *  3. Sauvegarder le bilan journalier dans day_summaries (historique)
 *  4. Purger le jour (clearDay)
 *
 * L'étape 3 est critique : c'est le seul moment où les tâches existent encore
 * en base pour calculer les statistiques. Après clearDay, elles sont perdues.
 *
 * Sécurité :
 *  - Aucune donnée utilisateur dans les Data d'entrée/sortie WorkManager
 *  - LocalDataSource singleton (SQLCipher AES-256)
 *  - applicationContext uniquement : pas de fuite d'Activity
 */
class MidnightResetWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = runCatching {
        val yesterday = DateUtils.yesterday()
        val local = LocalDataSource.getInstance(applicationContext)
        val userRepo = UserRepository(applicationContext)
        val taskRepo = TaskRepository(applicationContext)

        // 1. Charger AVANT suppression : evaluateStreakForDay a besoin de la liste
        val tasks = local.loadTasks(yesterday)

        // 2. Évaluation du streak (no-op si tasks est vide — jour de repos)
        userRepo.evaluateStreakForDay(yesterday, tasks)

        // 3. Sauvegarder le bilan journalier dans l'historique
        val streakCount = userRepo.loadStreak().count
        taskRepo.saveDaySummaryFromTasks(yesterday, tasks, streakCount)

        // 4. Suppression atomique tâches + session du jour écoulé (@Transaction dans LocalDataSource)
        local.clearDay(yesterday)

        Result.success()
    }.getOrElse {
        // Retry si erreur transitoire (I/O, SQLCipher temporairement indisponible)
        Result.retry()
    }
}
