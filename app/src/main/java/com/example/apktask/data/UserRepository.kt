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

    fun loadProfile(): UserProfile = local.loadProfile()

    fun saveProfile(profile: UserProfile) {
        local.saveProfile(profile)
        if (remote.isAvailable()) {
            // Sync asynchrone du profil public
        }
    }

    // ── Streak ───────────────────────────────────────────────────────────────

    fun loadStreak(): Streak = local.loadStreak()

    /**
     * Évalue la journée [date] et met à jour la série.
     * À appeler à minuit, AVANT de vider les tâches.
     *
     * Règles :
     *  - Toutes les tâches COMPLETED → incrémente la série
     *  - Au moins une non-complétée → remet la série à 0
     *  - Aucune tâche → ne change rien (jour de repos autorisé)
     */
    fun evaluateStreakForDay(date: String, tasks: List<Task>) {
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

    fun loadFriends(): List<FriendProgress> = local.loadFriends()

    /**
     * Ajoute un ami via son code (8 caractères du userId).
     * Nécessite Firebase — retourne une erreur explicite en mode mock.
     */
    suspend fun addFriendByCode(friendCode: String): Result<FriendProgress> =
        withContext(Dispatchers.IO) {
            if (!remote.isAvailable()) {
                return@withContext Result.failure(
                    UnsupportedOperationException(
                        "Connectez Firebase pour ajouter de vrais amis.\nVoir le guide d'intégration dans RemoteRepository.kt"
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

    fun removeFriend(userId: String) {
        local.saveFriends(local.loadFriends().filter { it.userId != userId })
    }

    /**
     * Récupère la progression journalière de tous les amis.
     * En mode mock : retourne les amis de démonstration [MockRemoteRepository.DEMO_FRIENDS].
     */
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
