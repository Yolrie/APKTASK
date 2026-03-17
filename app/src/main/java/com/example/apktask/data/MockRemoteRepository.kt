package com.example.apktask.data

import com.example.apktask.model.FriendProgress
import com.example.apktask.model.Task
import com.example.apktask.model.UserProfile

/**
 * Implémentation de démo de [RemoteRepository].
 *
 * Renvoie des données fictives pour que l'interface sociale soit testable
 * sans backend Firebase. Remplacez-la par FirebaseRemoteRepository en production.
 *
 * Les amis de démonstration sont clairement marqués [isMock = true]
 * et affichent un badge "Démo" dans l'interface.
 */
class MockRemoteRepository : RemoteRepository {

    override fun isAvailable(): Boolean = false

    override suspend fun syncDayTasks(date: String, tasks: List<Task>): Result<Unit> =
        Result.success(Unit)

    override suspend fun findUserByCode(friendCode: String): Result<UserProfile?> =
        Result.failure(UnsupportedOperationException("Firebase requis pour trouver des amis"))

    override suspend fun getFriendProgress(
        friendId: String,
        date: String
    ): Result<FriendProgress?> =
        Result.success(DEMO_FRIENDS.firstOrNull { it.userId == friendId })

    override suspend fun updatePublicProfile(profile: UserProfile): Result<Unit> =
        Result.success(Unit)

    companion object {
        /**
         * Amis de démonstration — illustrent l'interface sociale.
         * Supprimés automatiquement dès que Firebase est connecté.
         */
        val DEMO_FRIENDS = listOf(
            FriendProgress(
                userId = "demo_sophie",
                displayName = "Sophie",
                avatarColorIndex = 1,
                todayCompleted = 7,
                todayTotal = 9,
                streak = 12,
                isMock = true
            ),
            FriendProgress(
                userId = "demo_marc",
                displayName = "Marc",
                avatarColorIndex = 3,
                todayCompleted = 3,
                todayTotal = 8,
                streak = 2,
                isMock = true
            ),
            FriendProgress(
                userId = "demo_lea",
                displayName = "Léa",
                avatarColorIndex = 5,
                todayCompleted = 10,
                todayTotal = 10,
                streak = 21,
                isMock = true
            )
        )
    }
}
