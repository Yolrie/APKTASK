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
    fun getTasksForDate(date: String): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(tasks: List<TaskEntity>)

    @Query("DELETE FROM tasks WHERE date = :date")
    fun deleteForDate(date: String)

    @Query("DELETE FROM tasks")
    fun deleteAll()
}
