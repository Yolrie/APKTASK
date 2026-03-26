package com.example.apktask.data

import android.content.Context
import com.example.apktask.model.RecurringTask
import com.example.apktask.util.DateUtils

/** Repository des tâches récurrentes — CRUD sur les templates et calcul des templates dus pour une date. */
class RecurringTaskRepository(context: Context) {

    private val local = LocalDataSource.getInstance(context)

    // ── CRUD ─────────────────────────────────────────────────────────────────

    suspend fun add(task: RecurringTask): Int = local.saveRecurringTask(task)

    suspend fun getActive(): List<RecurringTask> = local.loadActiveRecurringTasks()

    suspend fun getAll(): List<RecurringTask> = local.loadAllRecurringTasks()

    suspend fun update(task: RecurringTask) = local.updateRecurringTask(task)

    /**
     * Suspend/reprend un template sans le supprimer (soft-delete).
     * Les tâches déjà injectées restent intactes dans l'historique.
     */
    suspend fun setActive(id: Int, active: Boolean) = local.setRecurringTaskActive(id, active)

    /** Suppression physique — à utiliser uniquement si l'utilisateur confirme. */
    suspend fun delete(id: Int) = local.deleteRecurringTask(id)

    // ── Injection ────────────────────────────────────────────────────────────

    /**
     * Retourne les templates actifs satisfaisant [date] et non encore injectés.
     * 2 requêtes DB (getActive + getInjectedRecurringTaskIds) quelle que soit la taille du catalogue.
     */
    suspend fun getDueForDate(date: String = DateUtils.today()): List<RecurringTask> {
        val alreadyInjected = local.loadInjectedRecurringTaskIds(date)
        return getActive().filter { task ->
            task.rule.isDueOn(date) && task.id !in alreadyInjected
        }
    }
}
