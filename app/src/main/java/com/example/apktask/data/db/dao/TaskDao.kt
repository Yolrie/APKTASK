package com.example.apktask.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.apktask.data.db.entity.TaskEntity

/**
 * DAO des tâches — requêtes filtrées par date.
 *
 * Stratégie de sauvegarde : deleteForDate() + insertAll() (upsert manuel)
 * plutôt qu'un REPLACE global pour éviter d'écraser des tâches d'autres jours.
 */
@Dao
interface TaskDao {

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY id ASC")
    suspend fun getTasksForDate(date: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE date = :date")
    suspend fun deleteForDate(date: String)

    @Query("DELETE FROM tasks")
    suspend fun deleteAll()

    /** Retourne en un seul aller-retour tous les recurring_task_id déjà injectés pour [date]. */
    @Query("SELECT DISTINCT recurring_task_id FROM tasks WHERE date = :date AND recurring_task_id IS NOT NULL")
    suspend fun getInjectedRecurringTaskIdsForDate(date: String): List<Int>
}
