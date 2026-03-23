package com.example.apktask.data

import android.content.Context
import com.example.apktask.model.DaySummary
import com.example.apktask.model.Task
import com.example.apktask.model.TaskStatus
import com.example.apktask.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Façade pour les opérations sur les tâches.
 *
 * Coordonne :
 *  - La persistance locale via [LocalDataSource] (SQLCipher, offline-first)
 *  - La synchronisation distante via [RemoteRepository] (si Firebase configuré)
 *
 * La sync distante est non-bloquante : l'app reste fonctionnelle hors-ligne.
 *
 * Scope de synchronisation :
 *  - [syncScope] est un [CoroutineScope] stable partagé entre toutes les instances
 *    de TaskRepository (companion object).
 *  - [SupervisorJob] : l'échec d'un job de sync n'annule pas les syncs suivantes.
 *  - La sync est "best-effort" : les exceptions sont swallowées via runCatching.
 *  - Ce scope ne doit pas être utilisé pour des opérations critiques (local-first).
 */
class TaskRepository(
    context: Context,
    private val remote: RemoteRepository = MockRemoteRepository()
) {
    private val local = LocalDataSource.getInstance(context)

    fun loadTasks(date: String = DateUtils.today()): List<Task> =
        local.loadTasks(date)

    fun saveTasks(date: String = DateUtils.today(), tasks: List<Task>) {
        local.saveTasks(date, tasks)
        if (remote.isAvailable()) {
            syncScope.launch {
                runCatching { remote.syncDayTasks(date, tasks) }
                // Exception swallowed : la sync est optionnelle, le local est source de vérité
            }
        }
    }

    fun loadSessionRegistered(date: String = DateUtils.today()): Boolean =
        local.loadSessionRegistered(date)

    fun saveSessionRegistered(date: String = DateUtils.today(), registered: Boolean) =
        local.saveSessionRegistered(date, registered)

    fun clearDay(date: String = DateUtils.today()) =
        local.clearDay(date)

    fun clearAll() =
        local.clearAll()

    // ── Bilans journaliers ──────────────────────────────────────────────────

    fun saveDaySummaryFromTasks(date: String, tasks: List<Task>, streakCount: Int) {
        if (tasks.isEmpty()) return
        val completed = tasks.count { it.status == TaskStatus.COMPLETED }
        val cancelled = tasks.count { it.status == TaskStatus.CANCELLED }
        val total = tasks.size
        val percent = if (total > 0) (completed * 100) / total else 0
        val summary = DaySummary(
            date = date,
            totalTasks = total,
            completedTasks = completed,
            cancelledTasks = cancelled,
            completionPercent = percent,
            allDone = completed == total,
            streakAtDay = streakCount
        )
        local.saveDaySummary(summary)
    }

    fun loadDaySummaries(): List<DaySummary> =
        local.loadDaySummaries()

    fun loadRecentDaySummaries(limit: Int = 30): List<DaySummary> =
        local.loadRecentDaySummaries(limit)

    fun daySummaryCount(): Int = local.daySummaryCount()
    fun averageCompletionPercent(): Int = local.averageCompletionPercent()
    fun perfectDaysCount(): Int = local.perfectDaysCount()

    companion object {
        /**
         * Scope stable pour les syncs distantes en arrière-plan.
         *
         * Utilisation d'un companion object (durée de vie = classe) plutôt que
         * de créer un nouveau CoroutineScope(Dispatchers.IO) à chaque saveTasks() :
         *  - Évite la création/destruction répétée de coroutine contexts
         *  - SupervisorJob : échec isolé par job, pas d'annulation en cascade
         *  - Dispatchers.IO : thread pool optimisé pour les I/O réseau
         */
        private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
