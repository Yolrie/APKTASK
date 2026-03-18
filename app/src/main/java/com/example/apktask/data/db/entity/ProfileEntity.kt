package com.example.apktask.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.apktask.model.UserProfile

/**
 * Entité Room représentant le profil de l'utilisateur local.
 *
 * Toujours une seule ligne (id = 1). L'@Insert avec OnConflictStrategy.REPLACE
 * se comporte comme un upsert — pas besoin d'@Update séparé.
 *
 * Sécurité : userId n'est jamais exposé dans les logs (AppDatabase.exportSchema = false).
 */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "avatar_color_index") val avatarColorIndex: Int,
    @ColumnInfo(name = "is_public") val isPublic: Boolean,
    @ColumnInfo(name = "notif_morning_enabled") val notifMorningEnabled: Boolean,
    @ColumnInfo(name = "notif_evening_enabled") val notifEveningEnabled: Boolean,
    @ColumnInfo(name = "notif_morning_hour") val notifMorningHour: Int,
    @ColumnInfo(name = "notif_evening_hour") val notifEveningHour: Int
) {
    fun toProfile() = UserProfile(
        userId = userId,
        displayName = displayName,
        avatarColorIndex = avatarColorIndex,
        isPublic = isPublic,
        notifMorningEnabled = notifMorningEnabled,
        notifEveningEnabled = notifEveningEnabled,
        notifMorningHour = notifMorningHour,
        notifEveningHour = notifEveningHour
    )
}

fun UserProfile.toEntity() = ProfileEntity(
    id = 1,
    userId = userId,
    displayName = displayName,
    avatarColorIndex = avatarColorIndex,
    isPublic = isPublic,
    notifMorningEnabled = notifMorningEnabled,
    notifEveningEnabled = notifEveningEnabled,
    notifMorningHour = notifMorningHour,
    notifEveningHour = notifEveningHour
)
