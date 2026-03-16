package com.example.apktask.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.example.apktask.data.TaskRepository
import com.example.apktask.data.UserRepository
import com.example.apktask.model.Streak
import com.example.apktask.model.Task
import com.example.apktask.model.TaskStatus
import com.example.apktask.util.DateUtils
import com.example.apktask.util.InputValidator
import java.util.Calendar

/**
 * ViewModel des tâches — source de vérité unique pour l'onglet Tâches.
 *
 * Responsabilités :
 *  - CRUD tâches + gestion session (register / reset)
 *  - Exposition de la série (streak) pour affichage dans l'en-tête
 *  - Message motivationnel quotidien (déterministe sur la date)
 *  - Persistance via TaskRepository (données chiffrées)
 *
 * Sécurité :
 *  - Toute entrée passe par InputValidator
 *  - MAX_TASKS = 10 (anti-abus)
 *  - Aucune donnée dans les logs
 */
class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TaskRepository(application)
    private val userRepository = UserRepository(application)

    val today: String = DateUtils.today()
    val todayReadable: String = DateUtils.todayReadable()

    // ── État interne ─────────────────────────────────────────────────────────

    private val _tasks = MutableLiveData<List<Task>>(emptyList())
    private val _editingIds = MutableLiveData<Set<Int>>(emptySet())

    val isSessionRegistered = MutableLiveData<Boolean>(false)
    val streak = MutableLiveData<Streak>(Streak())
    val errorMessage = MutableLiveData<String?>()

    /** Message motivationnel du jour (le même toute la journée). */
    val motivationalMessage: String = pickDailyMotivation()

    // ── État UI combiné ──────────────────────────────────────────────────────

    val tasksUiState: LiveData<List<TaskUiState>> =
        MediatorLiveData<List<TaskUiState>>().also { m ->
            m.addSource(_tasks) { tasks ->
                m.value = buildUiState(tasks, _editingIds.value.orEmpty())
            }
            m.addSource(_editingIds) { ids ->
                m.value = buildUiState(_tasks.value.orEmpty(), ids)
            }
        }

    private fun buildUiState(tasks: List<Task>, editingIds: Set<Int>): List<TaskUiState> =
        tasks.map { TaskUiState(task = it, isEditing = it.id in editingIds) }

    // ── Initialisation ───────────────────────────────────────────────────────

    init {
        _tasks.value = repository.loadTasks(today)
        isSessionRegistered.value = repository.loadSessionRegistered(today)
        streak.value = userRepository.loadStreak()
    }

    // ── Opérations CRUD ──────────────────────────────────────────────────────

    fun addTask(title: String) {
        when (val result = InputValidator.validateTitle(title)) {
            is InputValidator.Result.Failure -> errorMessage.value = result.reason
            is InputValidator.Result.Success -> {
                val current = _tasks.value.orEmpty()
                if (current.count { it.status == TaskStatus.DRAFT } >= MAX_TASKS) {
                    errorMessage.value = "Maximum $MAX_TASKS tâches autorisées par jour"
                    return
                }
                val newId = (current.maxOfOrNull { it.id } ?: 0) + 1
                _tasks.value = current + Task(id = newId, title = result.sanitized, date = today)
                persist()
            }
        }
    }

    fun startEditing(taskId: Int) {
        _editingIds.value = _editingIds.value.orEmpty() + taskId
    }

    fun cancelEditing(taskId: Int) {
        _editingIds.value = _editingIds.value.orEmpty() - taskId
    }

    fun saveEdit(taskId: Int, rawTitle: String) {
        when (val result = InputValidator.validateTitle(rawTitle)) {
            is InputValidator.Result.Failure -> errorMessage.value = result.reason
            is InputValidator.Result.Success -> {
                _tasks.value = _tasks.value.orEmpty().map { task ->
                    if (task.id == taskId) task.copy(title = result.sanitized) else task
                }
                _editingIds.value = _editingIds.value.orEmpty() - taskId
                persist()
            }
        }
    }

    fun deleteTask(taskId: Int) {
        _tasks.value = _tasks.value.orEmpty().filter { it.id != taskId }
        _editingIds.value = _editingIds.value.orEmpty() - taskId
        persist()
    }

    fun setStatus(taskId: Int, newStatus: TaskStatus) {
        _tasks.value = _tasks.value.orEmpty().map { task ->
            if (task.id == taskId) task.copy(status = newStatus) else task
        }
        persist()
    }

    // ── Gestion de session ────────────────────────────────────────────────────

    fun registerSession() {
        _tasks.value = _tasks.value.orEmpty().map { task ->
            if (task.status == TaskStatus.DRAFT) task.copy(status = TaskStatus.IN_PROGRESS)
            else task
        }
        _editingIds.value = emptySet()
        isSessionRegistered.value = true
        persist()
        repository.saveSessionRegistered(today, true)
    }

    fun resetAll() {
        repository.clearAll()
        _tasks.value = emptyList()
        _editingIds.value = emptySet()
        isSessionRegistered.value = false
        streak.value = userRepository.loadStreak()
    }

    // ── Utilitaires ──────────────────────────────────────────────────────────

    fun clearError() {
        errorMessage.value = null
    }

    private fun persist() {
        repository.saveTasks(today, _tasks.value.orEmpty())
    }

    /**
     * Choisit le message motivationnel du jour de façon déterministe
     * (numéro du jour dans l'année → index dans la liste).
     */
    private fun pickDailyMotivation(): String {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val messages = listOf(
            "Chaque tâche accomplie vous rapproche de vos objectifs 🎯",
            "Une journée productive commence par une liste claire 📋",
            "Petit à petit, l'oiseau fait son nid 🪺",
            "La discipline aujourd'hui, la liberté demain ✨",
            "Votre futur moi vous remerciera pour ce que vous faites maintenant 🚀",
            "Commencez par la tâche la plus difficile — le reste sera facile 💪",
            "Le succès, c'est la somme de petits efforts répétés chaque jour 🔥",
            "Vous n'avez pas besoin d'être parfait, juste constant 📈",
            "Une étape après l'autre — c'est ainsi que l'on avance loin 🏆",
            "Investir dans vos tâches du jour, c'est investir en vous 💡"
        )
        return messages[dayOfYear % messages.size]
    }

    companion object {
        const val MAX_TASKS = 10
    }
}
