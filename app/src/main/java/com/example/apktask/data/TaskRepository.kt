package com.example.apktask.data

import android.content.Context
import com.example.apktask.model.Task
import com.example.apktask.util.DateUtils
import com.example.apktask.util.InjectionPrefs
import com.example.apktask.widget.TaskWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Façade pour les opérations sur les tâches.
 * Toutes les fonctions sont suspend — les appelants lancent depuis viewModelScope ou syncScope.
 *
 * Injection des tâches récurrentes : voir [InjectionPrefs] pour la stratégie anti-doublon.
 */
class TaskRepository(
    private val context: Context,
    private val remote: RemoteRepository = MockRemoteRepository()
) {
    private val local = LocalDataSource.getInstance(context)
    private val recurringRepo = RecurringTaskRepository(context)

    // ── Tâches par date ──────────────────────────────────────────────────────

    /**
     * Charge les tâches de [date].
     * Pour aujourd'hui, les tâches récurrentes dues sont injectées avant le retour si ce n'est pas déjà fait.
     * La liste retournée inclut les éventuelles nouvelles tâches, sans second aller-retour en base.
     */
    suspend fun loadTasks(date: String = DateUtils.today()): List<Task> {
        if (date == DateUtils.today()) {
            val preloaded = injectDueRecurringTasksIfNeeded(date)
            if (preloaded != null) return preloaded
        }
        return local.loadTasks(date)
    }

    suspend fun saveTasks(date: String = DateUtils.today(), tasks: List<Task>) {
        local.saveTasks(date, tasks)
        TaskWidgetProvider.refreshAll(context)
        if (remote.isAvailable()) {
            syncScope.launch {
                runCatching { remote.syncDayTasks(date, tasks) }
            }
        }
    }

    suspend fun loadSessionRegistered(date: String = DateUtils.today()): Boolean =
        local.loadSessionRegistered(date)

    suspend fun saveSessionRegistered(date: String = DateUtils.today(), registered: Boolean) =
        local.saveSessionRegistered(date, registered)

    suspend fun clearDay(date: String = DateUtils.today()) =
        local.clearDay(date)

    suspend fun clearAll() =
        local.clearAll()

    // ── Injection des tâches récurrentes ─────────────────────────────────────

    /**
     * Injecte les tâches récurrentes dues pour [date], fusionne avec les existantes et retourne la liste finale.
     * IDs calculés à partir du max existant — les tâches du jour n'utilisent pas l'AUTOINCREMENT Room.
     * Appelé par [MidnightResetWorker] pour pré-peupler le nouveau jour.
     */
    suspend fun injectDueRecurringTasks(date: String = DateUtils.today()): List<Task> {
        val existing = local.loadTasks(date)
        val dueTasks = recurringRepo.getDueForDate(date)
        if (dueTasks.isEmpty()) return existing

        val baseId = existing.maxOfOrNull { it.id } ?: 0
        val injected = dueTasks.mapIndexed { index, recurring ->
            Task(
                id = baseId + index + 1,
                title = recurring.title,
                priority = recurring.priority,
                date = date,
                recurringTaskId = recurring.id
            )
        }
        val merged = existing + injected
        local.saveTasks(date, merged)
        TaskWidgetProvider.refreshAll(context)
        return merged
    }

    /** Retourne null si l'injection du jour est déjà faite (garde [InjectionPrefs]), sinon injecte et retourne la liste. */
    private suspend fun injectDueRecurringTasksIfNeeded(date: String): List<Task>? {
        if (InjectionPrefs.getLastInjectionDate(context) == date) return null
        val result = injectDueRecurringTasks(date)
        InjectionPrefs.setLastInjectionDate(context, date)
        return result
    }

    companion object {
        private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
