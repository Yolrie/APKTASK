package com.example.apktask.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.apktask.data.db.entity.ProfileEntity

@Dao
interface ProfileDao {

    /** Retourne null si aucun profil n'a encore été persisté (premier lancement). */
    @Query("SELECT * FROM profile WHERE id = 1 LIMIT 1")
    fun get(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun save(profile: ProfileEntity)
}
