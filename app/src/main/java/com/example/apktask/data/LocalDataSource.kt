package com.example.apktask.data

import android.content.Context
import androidx.room.withTransaction
import com.example.apktask.data.db.AppDatabase
import com.example.apktask.data.db.entity.SessionEntity
import com.example.apktask.data.db.entity.toEntity
import com.example.apktask.model.FriendProgress
import com.example.apktask.model.Streak
import com.example.apktask.model.Task
import com.example.apktask.model.UserProfile

/**
 * Source de données locale unique — toutes les données chiffrées au repos via SQLCipher.
 *
 * Migration off-main-thread :
 *  - Toutes les fonctions sont désormais `suspend`.
 *  - Les transactions utilisent [AppDatabase.withTransaction] (coroutine-aware)
 *    au lieu de [AppDatabase.runInTransaction] (bloquant).
 *  - [AppDatabase.allowMainThreadQueries()] est supprimé — toute tentative d'appel
 *    depuis le thread principal lèvera une IllegalStateException, détectable en dev.
 *
 * Atomicité préservée :
 *  - [saveTasks], [saveFriends], [clearDay], [clearAll] s'exécutent dans une
 *    transaction suspend via [withTransaction] — le comportement "tout ou rien" est
 *    identique à avant, mais sans bloquer le thread principal.
 */
class LocalDataSource private constructor(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val taskDao = db.taskDao()
    private val sessionDao = db.sessionDao()
    private val profileDao = db.profileDao()
    private val streakDao = db.streakDao()
    private val friendDao = db.friendDao()

    // ── Tâches par date ──────────────────────────────────────────────────────

    suspend fun saveTasks(date: String, tasks: List<Task>) {
        db.withTransaction {
            taskDao.deleteForDate(date)
            taskDao.insertAll(tasks.map { it.toEntity() })
        }
    }

    suspend fun loadTasks(date: String): List<Task> =
        taskDao.getTasksForDate(date).map { it.toTask() }

    suspend fun saveSessionRegistered(date: String, isRegistered: Boolean) {
        sessionDao.insert(SessionEntity(date = date, isRegistered = isRegistered))
    }

    suspend fun loadSessionRegistered(date: String): Boolean =
        sessionDao.isRegistered(date)

    /** Supprime les tâches et la session d'un jour donné (appelé par MidnightResetWorker). */
    suspend fun clearDay(date: String) {
        db.withTransaction {
            taskDao.deleteForDate(date)
            sessionDao.deleteForDate(date)
        }
    }

    // ── Profil utilisateur ───────────────────────────────────────────────────

    suspend fun saveProfile(profile: UserProfile) {
        profileDao.save(profile.toEntity())
    }

    suspend fun loadProfile(): UserProfile =
        profileDao.get()?.toProfile() ?: UserProfile()

    // ── Streak ───────────────────────────────────────────────────────────────

    suspend fun saveStreak(streak: Streak) {
        streakDao.save(streak.toEntity())
    }

    suspend fun loadStreak(): Streak =
        streakDao.get()?.toStreak() ?: Streak()

    // ── Amis ─────────────────────────────────────────────────────────────────

    suspend fun saveFriends(friends: List<FriendProgress>) {
        db.withTransaction {
            friendDao.deleteAll()
            friendDao.insertAll(friends.map { it.toEntity() })
        }
    }

    suspend fun loadFriends(): List<FriendProgress> =
        friendDao.getAll().map { it.toFriendProgress() }

    suspend fun deleteFriend(userId: String) {
        friendDao.deleteById(userId)
    }

    // ── Réinitialisation ─────────────────────────────────────────────────────

    /**
     * Supprime toutes les tâches et sessions (bouton "Reset" de l'utilisateur).
     * Ne touche pas au profil, au streak ni aux amis — données de long terme.
     */
    suspend fun clearAll() {
        db.withTransaction {
            taskDao.deleteAll()
            sessionDao.deleteAll()
        }
    }

    // ── Singleton ─────────────────────────────────────────────────────────────

    companion object {
        @Volatile
        private var INSTANCE: LocalDataSource? = null

        fun getInstance(context: Context): LocalDataSource =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalDataSource(context.applicationContext).also { INSTANCE = it }
            }
    }
}
