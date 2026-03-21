package com.example.apktask.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.apktask.data.LocalDataSource
import com.example.apktask.data.TaskRepository
import com.example.apktask.data.UserRepository
import com.example.apktask.util.DateUtils
import com.example.apktask.util.InjectionPrefs

/**
 * Worker exécuté à minuit pour évaluer le streak puis purger les tâches du jour écoulé.
 *
 * Ordre impératif :
 *  1. Charger les tâches du jour écoulé (encore présentes en base)
 *  2. Évaluer le streak avec ces tâches (requiert la liste pour décider COMPLETED ou non)
 *  3. Purger le jour (clearDay)
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
        val today = DateUtils.today()
        val local = LocalDataSource.getInstance(applicationContext)
        val taskRepository = TaskRepository(applicationContext)

        // 1. Charger AVANT suppression : evaluateStreakForDay a besoin de la liste
        val tasks = local.loadTasks(yesterday)

        // 2. Évaluation du streak (no-op si tasks est vide — jour de repos)
        UserRepository(applicationContext).evaluateStreakForDay(yesterday, tasks)

        // 3. Suppression atomique tâches + session du jour écoulé (@Transaction dans LocalDataSource)
        local.clearDay(yesterday)

        // 4. Injection des tâches récurrentes pour le nouveau jour.
        //    InjectionPrefs est mis à jour pour que l'ouverture de l'app ne re-injecte pas.
        taskRepository.injectDueRecurringTasks(today)
        InjectionPrefs.setLastInjectionDate(applicationContext, today)

        Result.success()
    }.getOrElse {
        // Retry si erreur transitoire (I/O, SQLCipher temporairement indisponible)
        Result.retry()
    }
}
