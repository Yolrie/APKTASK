package com.example.apktask.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.apktask.data.db.entity.FriendEntity

@Dao
interface FriendDao {

    @Query("SELECT * FROM friends ORDER BY display_name ASC")
    fun getAll(): List<FriendEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(friends: List<FriendEntity>)

    @Query("DELETE FROM friends WHERE user_id = :userId")
    fun deleteById(userId: String)

    @Query("DELETE FROM friends")
    fun deleteAll()
}
