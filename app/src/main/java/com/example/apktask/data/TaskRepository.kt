package com.example.apktask.data

import android.content.Context
import com.example.apktask.model.Task
import com.example.apktask.util.DateUtils
import com.example.apktask.widget.TaskWidgetProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Façade pour les opérations sur les tâches.
 *
 * Toutes les fonctions sont suspend — les appelants lancent depuis
 * viewModelScope ou syncScope selon les besoins.
 */
class TaskRepository(
    private val context: Context,
    private val remote: RemoteRepository = MockRemoteRepository()
) {
    private val local = LocalDataSource.getInstance(context)

    suspend fun loadTasks(date: String = DateUtils.today()): List<Task> =
        local.loadTasks(date)

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

    companion object {
        private val syncScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    }
}
