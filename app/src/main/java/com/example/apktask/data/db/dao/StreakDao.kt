package com.example.apktask.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.apktask.data.db.entity.StreakEntity

@Dao
interface StreakDao {

    /** Retourne null si aucune série n'a encore été persistée. */
    @Query("SELECT * FROM streak WHERE id = 1 LIMIT 1")
    fun get(): StreakEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(streak: StreakEntity)
}
