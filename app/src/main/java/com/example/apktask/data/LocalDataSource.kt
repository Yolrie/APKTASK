package com.example.apktask.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.apktask.model.FriendProgress
import com.example.apktask.model.Streak
import com.example.apktask.model.Task
import com.example.apktask.model.TaskStatus
import com.example.apktask.model.UserProfile
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.util.UUID

/**
 * Source de données locale unique — toutes les données chiffrées au repos.
 *
 * Architecture des clés :
 *  - "tasks_YYYY-MM-DD"   → tâches du jour (JSON array)
 *  - "session_YYYY-MM-DD" → session enregistrée pour ce jour (boolean)
 *  - "profile"            → profil utilisateur (JSON object)
 *  - "streak"             → série courante (JSON object)
 *  - "friends"            → liste des amis (JSON array)
 *
 * Sécurité :
 *  - AES-256-GCM pour les valeurs, AES-256-SIV pour les clés
 *  - Singleton : une seule instance de EncryptedSharedPreferences dans la JVM
 *  - applicationContext pour éviter toute fuite d'Activity
 *  - Aucune donnée utilisateur dans les logs
 */
class LocalDataSource private constructor(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val key = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            PREFS_NAME,
            key,
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ── Tâches par date ──────────────────────────────────────────────────────

    fun saveTasks(date: String, tasks: List<Task>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(JSONObject().apply {
                put(K_ID, task.id)
                put(K_TITLE, task.title)
                put(K_CREATED, task.createdAt)
                put(K_STATUS, task.status.code)
                put(K_DATE, task.date)
            })
        }
        prefs.edit().putString(taskKey(date), array.toString()).apply()
    }

    fun loadTasks(date: String): List<Task> {
        val json = prefs.getString(taskKey(date), "[]") ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                runCatching {
                    val o = array.getJSONObject(i)
                    Task(
                        id = o.getInt(K_ID),
                        title = o.getString(K_TITLE),
                        createdAt = o.getLong(K_CREATED),
                        status = TaskStatus.fromCode(o.getInt(K_STATUS)),
                        date = o.optString(K_DATE, date)
                    )
                }.getOrNull()
            }
        } catch (_: JSONException) {
            emptyList()
        }
    }

    fun saveSessionRegistered(date: String, isRegistered: Boolean) {
        prefs.edit().putBoolean(sessionKey(date), isRegistered).apply()
    }

    fun loadSessionRegistered(date: String): Boolean =
        prefs.getBoolean(sessionKey(date), false)

    fun clearDay(date: String) {
        prefs.edit().remove(taskKey(date)).remove(sessionKey(date)).apply()
    }

    // ── Profil utilisateur ───────────────────────────────────────────────────

    fun saveProfile(profile: UserProfile) {
        val json = JSONObject().apply {
            put("userId", profile.userId)
            put("displayName", profile.displayName)
            put("avatarColorIndex", profile.avatarColorIndex)
            put("isPublic", profile.isPublic)
            put("notifMorningEnabled", profile.notifMorningEnabled)
            put("notifEveningEnabled", profile.notifEveningEnabled)
            put("notifMorningHour", profile.notifMorningHour)
            put("notifEveningHour", profile.notifEveningHour)
        }
        prefs.edit().putString(KEY_PROFILE, json.toString()).apply()
    }

    fun loadProfile(): UserProfile {
        val json = prefs.getString(KEY_PROFILE, null) ?: return UserProfile()
        return try {
            val o = JSONObject(json)
            UserProfile(
                userId = o.optString("userId").ifBlank { UUID.randomUUID().toString() },
                displayName = o.optString("displayName", ""),
                avatarColorIndex = o.optInt("avatarColorIndex", 0),
                isPublic = o.optBoolean("isPublic", false),
                notifMorningEnabled = o.optBoolean("notifMorningEnabled", true),
                notifEveningEnabled = o.optBoolean("notifEveningEnabled", true),
                notifMorningHour = o.optInt("notifMorningHour", 8),
                notifEveningHour = o.optInt("notifEveningHour", 21)
            )
        } catch (_: JSONException) {
            UserProfile()
        }
    }

    // ── Streak ───────────────────────────────────────────────────────────────

    fun saveStreak(streak: Streak) {
        val json = JSONObject().apply {
            put("count", streak.count)
            put("lastCountedDate", streak.lastCountedDate)
            put("longestEver", streak.longestEver)
        }
        prefs.edit().putString(KEY_STREAK, json.toString()).apply()
    }

    fun loadStreak(): Streak {
        val json = prefs.getString(KEY_STREAK, null) ?: return Streak()
        return try {
            val o = JSONObject(json)
            Streak(
                count = o.optInt("count", 0),
                lastCountedDate = o.optString("lastCountedDate", ""),
                longestEver = o.optInt("longestEver", 0)
            )
        } catch (_: JSONException) {
            Streak()
        }
    }

    // ── Amis ─────────────────────────────────────────────────────────────────

    fun saveFriends(friends: List<FriendProgress>) {
        val array = JSONArray()
        friends.forEach { f ->
            array.put(JSONObject().apply {
                put("userId", f.userId)
                put("displayName", f.displayName)
                put("avatarColorIndex", f.avatarColorIndex)
                put("streak", f.streak)
            })
        }
        prefs.edit().putString(KEY_FRIENDS, array.toString()).apply()
    }

    fun loadFriends(): List<FriendProgress> {
        val json = prefs.getString(KEY_FRIENDS, "[]") ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                runCatching {
                    val o = array.getJSONObject(i)
                    FriendProgress(
                        userId = o.getString("userId"),
                        displayName = o.getString("displayName"),
                        avatarColorIndex = o.optInt("avatarColorIndex", 0),
                        streak = o.optInt("streak", 0)
                    )
                }.getOrNull()
            }
        } catch (_: JSONException) {
            emptyList()
        }
    }

    // ── Réinitialisation ─────────────────────────────────────────────────────

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun taskKey(date: String) = "tasks_$date"
    private fun sessionKey(date: String) = "session_$date"

    // ── Singleton ─────────────────────────────────────────────────────────────

    companion object {
        private const val PREFS_NAME = "apktask_v3_secure"
        private const val KEY_PROFILE = "profile"
        private const val KEY_STREAK = "streak"
        private const val KEY_FRIENDS = "friends"
        private const val K_ID = "id"
        private const val K_TITLE = "title"
        private const val K_CREATED = "createdAt"
        private const val K_STATUS = "status"
        private const val K_DATE = "date"

        @Volatile
        private var INSTANCE: LocalDataSource? = null

        fun getInstance(context: Context): LocalDataSource =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: LocalDataSource(context.applicationContext).also { INSTANCE = it }
            }
    }
}
