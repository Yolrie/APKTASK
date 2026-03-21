package com.example.apktask.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.apktask.data.db.entity.RecurringTaskEntity

/**
 * DAO des tâches récurrentes (templates).
 *
 * Les tâches récurrentes ne sont jamais supprimées physiquement (soft-delete via [setActive])
 * pour préserver la traçabilité entre [RecurringTaskEntity] et les [TaskEntity] injectées.
 */
@Dao
interface RecurringTaskDao {

    @Query("SELECT * FROM recurring_tasks WHERE is_active = 1 ORDER BY created_at ASC")
    suspend fun getActive(): List<RecurringTaskEntity>

    @Query("SELECT * FROM recurring_tasks ORDER BY created_at ASC")
    suspend fun getAll(): List<RecurringTaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: RecurringTaskEntity): Long

    @Update
    suspend fun update(task: RecurringTaskEntity)

    @Query("UPDATE recurring_tasks SET is_active = :active WHERE id = :id")
    suspend fun setActive(id: Int, active: Boolean)

    @Query("DELETE FROM recurring_tasks WHERE id = :id")
    suspend fun deleteById(id: Int)
}
