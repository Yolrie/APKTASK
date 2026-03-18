package com.example.apktask.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Entité Room représentant l'état de session d'un jour donné.
 *
 * Une session est "enregistrée" quand l'utilisateur a validé sa liste du jour.
 * Clé primaire = date (format YYYY-MM-DD) : unicité par jour garantie sans
 * auto-incrément superflu.
 */
@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val date: String,
    @ColumnInfo(name = "is_registered") val isRegistered: Boolean
)
