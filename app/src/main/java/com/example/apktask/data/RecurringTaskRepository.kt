package com.example.apktask.data

import android.content.Context
import com.example.apktask.model.RecurringTask
import com.example.apktask.util.DateUtils

/**
 * Repository des tâches récurrentes (templates de routines).
 *
 * Responsabilités :
 *  - CRUD sur les templates [RecurringTask]
 *  - Calcul des templates dus pour une date donnée ([getDueForDate])
 *
 * Anti-doublon à deux niveaux :
 *  1. [getDueForDate] filtre les templates dont l'injection du jour est déjà en base
 *     (via [LocalDataSource.isRecurringTaskInjectedForDate] → TaskDao.hasRecurringTaskForDate).
 *  2. [com.example.apktask.util.InjectionPrefs] évite même d'appeler [getDueForDate]
 *     si l'injection du jour a déjà eu lieu (vérification rapide en SharedPreferences).
 */
class RecurringTaskRepository(context: Context) {

    private val local = LocalDataSource.getInstance(context)

    // ── CRUD ─────────────────────────────────────────────────────────────────

    /**
     * Insère un nouveau template et retourne son id auto-généré.
     * Si [task.id] == 0, Room génère l'id via AUTOINCREMENT.
     */
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
     * Retourne les templates actifs dont :
     *  - La règle [RecurringTask.rule] est satisfaite pour [date]
     *  - Aucune tâche issue de ce template n'existe déjà pour [date] en base
     *
     * @param date Date ISO-8601 (yyyy-MM-dd), aujourd'hui par défaut.
     */
    suspend fun getDueForDate(date: String = DateUtils.today()): List<RecurringTask> =
        getActive().filter { task ->
            task.rule.isDueOn(date) && !local.isRecurringTaskInjectedForDate(task.id, date)
        }
}
