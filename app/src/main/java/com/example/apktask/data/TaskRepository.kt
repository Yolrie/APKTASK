package com.example.apktask.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.example.apktask.model.Task
import com.example.apktask.model.TaskStatus
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Couche d'accès aux données.
 *
 * Sécurité :
 *  - Les données sont chiffrées au repos via EncryptedSharedPreferences
 *    (AES-256-GCM pour les valeurs, AES-256-SIV pour les clés).
 *  - Aucune donnée utilisateur n'est écrite dans les logs.
 *  - applicationContext est utilisé pour éviter les fuites de mémoire.
 */
class TaskRepository(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            PREFS_FILE_NAME,
            masterKeyAlias,
            context.applicationContext,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    // ── Tâches ──────────────────────────────────────────────────────────────

    fun saveTasks(tasks: List<Task>) {
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(
                JSONObject().apply {
                    put(KEY_ID, task.id)
                    put(KEY_TITLE, task.title)
                    put(KEY_CREATED_AT, task.createdAt)
                    put(KEY_STATUS, task.status.code)
                }
            )
        }
        prefs.edit().putString(KEY_TASKS_LIST, array.toString()).apply()
    }

    fun loadTasks(): List<Task> {
        val json = prefs.getString(KEY_TASKS_LIST, "[]") ?: return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).mapNotNull { i ->
                parseTask(array.getJSONObject(i))
            }
        } catch (_: JSONException) {
            emptyList()
        }
    }

    private fun parseTask(obj: JSONObject): Task? = try {
        Task(
            id = obj.getInt(KEY_ID),
            title = obj.getString(KEY_TITLE),
            createdAt = obj.getLong(KEY_CREATED_AT),
            status = TaskStatus.fromCode(obj.getInt(KEY_STATUS))
        )
    } catch (_: Exception) {
        null
    }

    // ── État de session ──────────────────────────────────────────────────────

    fun saveSessionRegistered(isRegistered: Boolean) {
        prefs.edit().putBoolean(KEY_SESSION_REGISTERED, isRegistered).apply()
    }

    fun loadSessionRegistered(): Boolean =
        prefs.getBoolean(KEY_SESSION_REGISTERED, false)

    // ── Réinitialisation ─────────────────────────────────────────────────────

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    // ── Constantes ───────────────────────────────────────────────────────────

    companion object {
        private const val PREFS_FILE_NAME = "apktask_secure_prefs"
        private const val KEY_TASKS_LIST = "tasks_list"
        private const val KEY_SESSION_REGISTERED = "session_registered"
        private const val KEY_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_CREATED_AT = "created_at"
        private const val KEY_STATUS = "status"
    }
}
