package com.example.apktask.data

import android.content.Context
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
 * Remplacement de l'ancienne implémentation basée sur EncryptedSharedPreferences + JSON :
 *
 *  | Avant (v3)                      | Maintenant (v3.1+)                        |
 *  |---------------------------------|-------------------------------------------|
 *  | EncryptedSharedPreferences      | Room + SQLCipher AES-256 page-level       |
 *  | Sérialisation JSON manuelle     | Entités typées + DAO Room                 |
 *  | Clés String "tasks_YYYY-MM-DD"  | Table `tasks` avec index sur `date`       |
 *  | Pas d'index, full scan          | Index SQL natif sur les colonnes requêtées|
 *
 * Sécurité :
 *  - [AppDatabase] ouvre la base via [net.sqlcipher.database.SupportFactory] :
 *    chiffrement AES-256 appliqué à chaque page SQLite (~4 Ko).
 *  - La passphrase SQLCipher est gérée par [com.example.apktask.data.db.DatabaseKeyManager]
 *    (générée une fois, stockée chiffrée dans un EncryptedSharedPreferences dédié).
 *  - Singleton thread-safe : une seule instance d'AppDatabase dans la JVM.
 *  - applicationContext uniquement : pas de fuite d'Activity.
 *  - Aucune donnée utilisateur dans les logs.
 *
 * Note : [AppDatabase.allowMainThreadQueries()] est actif — les appels depuis les
 * init{} des ViewModels sont synchrones. Migration en suspend fun prévue.
 */
class LocalDataSource private constructor(context: Context) {

    private val db = AppDatabase.getInstance(context)
    private val taskDao = db.taskDao()
    private val sessionDao = db.sessionDao()
    private val profileDao = db.profileDao()
    private val streakDao = db.streakDao()
    private val friendDao = db.friendDao()

    // ── Tâches par date ──────────────────────────────────────────────────────

    fun saveTasks(date: String, tasks: List<Task>) {
        taskDao.deleteForDate(date)
        taskDao.insertAll(tasks.map { it.toEntity() })
    }

    fun loadTasks(date: String): List<Task> =
        taskDao.getTasksForDate(date).map { it.toTask() }

    fun saveSessionRegistered(date: String, isRegistered: Boolean) {
        sessionDao.insert(SessionEntity(date = date, isRegistered = isRegistered))
    }

    fun loadSessionRegistered(date: String): Boolean =
        sessionDao.isRegistered(date)

    /** Supprime les tâches et la session d'un jour donné (appelé par MidnightResetWorker). */
    fun clearDay(date: String) {
        taskDao.deleteForDate(date)
        sessionDao.deleteForDate(date)
    }

    // ── Profil utilisateur ───────────────────────────────────────────────────

    fun saveProfile(profile: UserProfile) {
        profileDao.save(profile.toEntity())
    }

    fun loadProfile(): UserProfile =
        profileDao.get()?.toProfile() ?: UserProfile()

    // ── Streak ───────────────────────────────────────────────────────────────

    fun saveStreak(streak: Streak) {
        streakDao.save(streak.toEntity())
    }

    fun loadStreak(): Streak =
        streakDao.get()?.toStreak() ?: Streak()

    // ── Amis ─────────────────────────────────────────────────────────────────

    fun saveFriends(friends: List<FriendProgress>) {
        friendDao.deleteAll()
        friendDao.insertAll(friends.map { it.toEntity() })
    }

    fun loadFriends(): List<FriendProgress> =
        friendDao.getAll().map { it.toFriendProgress() }

    fun deleteFriend(userId: String) {
        friendDao.deleteById(userId)
    }

    // ── Réinitialisation ─────────────────────────────────────────────────────

    /**
     * Supprime toutes les tâches et sessions (bouton "Reset" de l'utilisateur).
     * Ne touche pas au profil, au streak ni aux amis — données de long terme.
     */
    fun clearAll() {
        taskDao.deleteAll()
        sessionDao.deleteAll()
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
