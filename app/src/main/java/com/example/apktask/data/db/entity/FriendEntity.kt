package com.example.apktask.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.apktask.model.FriendProgress

/**
 * Entité Room représentant un ami persisté localement.
 *
 * Seuls les champs "statiques" sont persistés (identité + série) :
 *  - [todayCompleted] / [todayTotal] / [isMock] sont des données runtime/remote,
 *    recalculées à chaque chargement depuis le dépôt distant ou MockRepository.
 *
 * Sécurité : userId est la clé primaire — pas de doublon possible.
 */
@Entity(tableName = "friends")
data class FriendEntity(
    @PrimaryKey @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "display_name") val displayName: String,
    @ColumnInfo(name = "avatar_color_index") val avatarColorIndex: Int,
    val streak: Int
) {
    fun toFriendProgress() = FriendProgress(
        userId = userId,
        displayName = displayName,
        avatarColorIndex = avatarColorIndex,
        streak = streak
    )
}

fun FriendProgress.toEntity() = FriendEntity(
    userId = userId,
    displayName = displayName,
    avatarColorIndex = avatarColorIndex,
    streak = streak
)
