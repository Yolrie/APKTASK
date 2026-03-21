package com.example.apktask.data

import android.content.Context
import com.example.apktask.model.FriendProgress
import com.example.apktask.model.Streak
import com.example.apktask.model.Task
import com.example.apktask.model.TaskStatus
import com.example.apktask.model.UserProfile
import com.example.apktask.util.DateUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Gère le profil utilisateur, la série (streak) et la liste des amis.
 *
 * Toutes les fonctions sont suspend et s'exécutent sur Dispatchers.IO
 * (sauf quand elles délèguent à withContext(Dispatchers.IO) elles-mêmes).
 *
 * Sécurité :
 *  - addFriendByCode() est un no-op sans Firebase (évite de faux positifs)
 *  - removeFriend() supprime localement — immédiat, pas de confirmation réseau
 */
class UserRepository(
    context: Context,
    private val remote: RemoteRepository = MockRemoteRepository()
) {
    private val local = LocalDataSource.getInstance(context)

    // ── Profil ───────────────────────────────────────────────────────────────

    suspend fun loadProfile(): UserProfile = local.loadProfile()

    suspend fun saveProfile(profile: UserProfile) {
        local.saveProfile(profile)
    }

    // ── Streak ───────────────────────────────────────────────────────────────

    suspend fun loadStreak(): Streak = local.loadStreak()

    /**
     * Évalue la journée [date] et met à jour la série.
     * À appeler à minuit, AVANT de vider les tâches.
     */
    suspend fun evaluateStreakForDay(date: String, tasks: List<Task>) {
        if (tasks.isEmpty()) return

        val current = local.loadStreak()
        val allDone = tasks.all { it.status == TaskStatus.COMPLETED }

        val newStreak = if (allDone) {
            if (current.lastCountedDate == date) {
                current // Déjà comptabilisé aujourd'hui
            } else {
                val consecutive = DateUtils.areConsecutiveDays(current.lastCountedDate, date)
                val newCount = if (consecutive) current.count + 1 else 1
                Streak(
                    count = newCount,
                    lastCountedDate = date,
                    longestEver = maxOf(current.longestEver, newCount)
                )
            }
        } else {
            Streak(count = 0, lastCountedDate = date, longestEver = current.longestEver)
        }

        local.saveStreak(newStreak)
    }

    // ── Amis ─────────────────────────────────────────────────────────────────

    suspend fun loadFriends(): List<FriendProgress> = local.loadFriends()

    suspend fun addFriendByCode(friendCode: String): Result<FriendProgress> =
        withContext(Dispatchers.IO) {
            if (!remote.isAvailable()) {
                return@withContext Result.failure(
                    UnsupportedOperationException(
                        "Connectez Firebase pour ajouter de vrais amis."
                    )
                )
            }
            remote.findUserByCode(friendCode).mapCatching { profile ->
                val p = profile ?: error("Aucun utilisateur trouvé pour ce code")
                val friend = FriendProgress(
                    userId = p.userId,
                    displayName = p.displayName,
                    avatarColorIndex = p.avatarColorIndex
                )
                local.saveFriends(local.loadFriends() + friend)
                friend
            }
        }

    suspend fun removeFriend(userId: String) {
        local.deleteFriend(userId)
    }

    suspend fun getFriendsProgress(date: String = DateUtils.today()): List<FriendProgress> =
        withContext(Dispatchers.IO) {
            if (!remote.isAvailable()) {
                return@withContext MockRemoteRepository.DEMO_FRIENDS
            }
            local.loadFriends().mapNotNull { friend ->
                remote.getFriendProgress(friend.userId, date).getOrNull() ?: friend
            }
        }
}
