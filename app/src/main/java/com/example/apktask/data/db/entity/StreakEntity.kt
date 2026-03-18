package com.example.apktask.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.apktask.model.Streak

/**
 * Entité Room représentant la série courante de l'utilisateur.
 *
 * Toujours une seule ligne (id = 1), mise à jour via REPLACE.
 * [badge] est un champ calculé dans le modèle — non persisté.
 */
@Entity(tableName = "streak")
data class StreakEntity(
    @PrimaryKey val id: Int = 1,
    val count: Int,
    @ColumnInfo(name = "last_counted_date") val lastCountedDate: String,
    @ColumnInfo(name = "longest_ever") val longestEver: Int
) {
    fun toStreak() = Streak(
        count = count,
        lastCountedDate = lastCountedDate,
        longestEver = longestEver
    )
}

fun Streak.toEntity() = StreakEntity(
    id = 1,
    count = count,
    lastCountedDate = lastCountedDate,
    longestEver = longestEver
)
