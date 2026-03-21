package com.example.apktask.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.apktask.data.db.entity.SessionEntity

@Dao
interface SessionDao {

    @Query("SELECT is_registered FROM sessions WHERE date = :date LIMIT 1")
    suspend fun isRegistered(date: String): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity)

    @Query("DELETE FROM sessions WHERE date = :date")
    suspend fun deleteForDate(date: String)

    @Query("DELETE FROM sessions")
    suspend fun deleteAll()
}
