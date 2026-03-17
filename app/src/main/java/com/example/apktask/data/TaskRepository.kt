package com.example.apktask.data

import android.content.Context
import com.example.apktask.model.Task
import com.example.apktask.util.DateUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Façade pour les opérations sur les tâches.
 *
 * Coordonne :
 *  - La persistance locale via [LocalDataSource] (chiffrée, offline-first)
 *  - La synchronisation distante via [RemoteRepository] (si Firebase configuré)
 *
 * La sync distante est non-bloquante : l'app reste fonctionnelle hors-ligne.
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
            CoroutineScope(Dispatchers.IO).launch {
                remote.syncDayTasks(date, tasks)
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
}
