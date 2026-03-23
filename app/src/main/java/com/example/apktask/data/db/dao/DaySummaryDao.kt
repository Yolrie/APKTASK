package com.example.apktask.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.apktask.data.db.entity.DaySummaryEntity

/**
 * DAO pour les bilans journaliers.
 *
 * Les bilans sont insérés une fois par jour (minuit) et ne sont jamais modifiés.
 * REPLACE gère le cas rare d'un double déclenchement du worker.
 */
@Dao
interface DaySummaryDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(summary: DaySummaryEntity)

    @Query("SELECT * FROM day_summaries ORDER BY date DESC")
    fun getAll(): List<DaySummaryEntity>

    @Query("SELECT * FROM day_summaries ORDER BY date DESC LIMIT :limit")
    fun getRecent(limit: Int): List<DaySummaryEntity>

    @Query("SELECT * FROM day_summaries WHERE date = :date LIMIT 1")
    fun getForDate(date: String): DaySummaryEntity?

    @Query("SELECT * FROM day_summaries WHERE date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    fun getRange(startDate: String, endDate: String): List<DaySummaryEntity>

    @Query("SELECT COUNT(*) FROM day_summaries")
    fun count(): Int

    @Query("SELECT AVG(completion_percent) FROM day_summaries")
    fun averageCompletionPercent(): Int

    @Query("SELECT COUNT(*) FROM day_summaries WHERE all_done = 1")
    fun countPerfectDays(): Int

    @Query("DELETE FROM day_summaries")
    fun deleteAll()
}
