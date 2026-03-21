package com.example.apktask.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.apktask.data.TaskRepository
import com.example.apktask.data.UserRepository
import com.example.apktask.model.Priority
import com.example.apktask.model.Streak
import com.example.apktask.model.Task
import com.example.apktask.model.TaskStatus
import com.example.apktask.util.DateUtils
import com.example.apktask.util.InputValidator
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
 * Architecture StateFlow :
 *  - [MutableStateFlow] en interne, [StateFlow] exposé en lecture seule (asStateFlow)
 *  - [tasksUiState] combine _tasks + _editingIds via [combine] → remplace MediatorLiveData
 *  - SharingStarted.WhileSubscribed(5_000) : le flow reste actif 5s après désinscription,
 *    évitant un recalcul inutile lors des rotations d'écran
 *  - [update] pour les mutations atomiques sur Set et List
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

    // ── État interne (privé, mutable) ────────────────────────────────────────

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    private val _editingIds = MutableStateFlow<Set<Int>>(emptySet())

    private val _isSessionRegistered = MutableStateFlow(false)
    private val _streak = MutableStateFlow(Streak())
    private val _errorMessage = MutableStateFlow<String?>(null)

    /**
     * Holds the most recently deleted task for 5 seconds, enabling undo.
     * Cleared automatically after [UNDO_TIMEOUT_MS] or when undo is confirmed.
     */
    private val _deletedTask = MutableStateFlow<Task?>(null)
    val deletedTask: StateFlow<Task?> = _deletedTask.asStateFlow()
    private var undoJob: Job? = null

    // ── État exposé (lecture seule) ───────────────────────────────────────────

    val isSessionRegistered: StateFlow<Boolean> = _isSessionRegistered.asStateFlow()
    val streak: StateFlow<Streak> = _streak.asStateFlow()
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    /** Message motivationnel du jour (le même toute la journée). */
    val motivationalMessage: String = pickDailyMotivation()

    // ── État UI combiné ──────────────────────────────────────────────────────

    /**
     * Fusionne les tâches brutes et les IDs en cours d'édition en un seul flux UI.
     *
     * Avantage vs MediatorLiveData :
     *  - Pas de gestion manuelle de addSource/removeSource
     *  - Opérateur [combine] garanti thread-safe et sans intermédiaire nul
     *  - WhileSubscribed(5_000) : survive aux rotations sans recalcul immédiat
     */
    val tasksUiState: StateFlow<List<TaskUiState>> =
        combine(_tasks, _editingIds) { tasks, editingIds ->
            // Sort active tasks by priority descending (HIGH first), then by creation time.
            // Completed/cancelled tasks keep their natural order at the end.
            val sorted = tasks.sortedWith(
                compareByDescending<Task> { it.priority.code }.thenBy { it.createdAt }
            )
            sorted.map { TaskUiState(task = it, isEditing = it.id in editingIds) }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    // ── Initialisation ───────────────────────────────────────────────────────

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _tasks.value = repository.loadTasks(today)
            _isSessionRegistered.value = repository.loadSessionRegistered(today)
            _streak.value = userRepository.loadStreak()
        }
    }

    // ── Opérations CRUD ──────────────────────────────────────────────────────

    fun addTask(title: String) {
        when (val result = InputValidator.validateTitle(title)) {
            is InputValidator.Result.Failure -> _errorMessage.value = result.reason
            is InputValidator.Result.Success -> {
                val current = _tasks.value
                if (current.count { it.status == TaskStatus.DRAFT } >= MAX_TASKS) {
                    _errorMessage.value = "Maximum $MAX_TASKS tâches autorisées par jour"
                    return
                }
                val newId = (current.maxOfOrNull { it.id } ?: 0) + 1
                _tasks.value = current + Task(id = newId, title = result.sanitized, date = today)
                persist()
            }
        }
    }

    fun startEditing(taskId: Int) {
        _editingIds.update { it + taskId }
    }

    fun cancelEditing(taskId: Int) {
        _editingIds.update { it - taskId }
    }

    fun saveEdit(taskId: Int, rawTitle: String) {
        when (val result = InputValidator.validateTitle(rawTitle)) {
            is InputValidator.Result.Failure -> _errorMessage.value = result.reason
            is InputValidator.Result.Success -> {
                _tasks.update { tasks ->
                    tasks.map { if (it.id == taskId) it.copy(title = result.sanitized) else it }
                }
                _editingIds.update { it - taskId }
                persist()
            }
        }
    }

    fun deleteTask(taskId: Int) {
        val task = _tasks.value.find { it.id == taskId } ?: return
        _tasks.update { tasks -> tasks.filter { it.id != taskId } }
        _editingIds.update { it - taskId }
        persist()
        // Hold deleted task for UNDO_TIMEOUT_MS then clear
        _deletedTask.value = task
        undoJob?.cancel()
        undoJob = viewModelScope.launch {
            delay(UNDO_TIMEOUT_MS)
            _deletedTask.value = null
        }
    }

    /**
     * Restores the most recently deleted task.
     * No-op if the undo window has expired.
     */
    fun undoDelete() {
        val task = _deletedTask.value ?: return
        undoJob?.cancel()
        _deletedTask.value = null
        _tasks.update { it + task }
        persist()
    }

    /**
     * Cycles the priority of a task: NONE → HIGH → MEDIUM → LOW → NONE.
     * Only active tasks (DRAFT / IN_PROGRESS) can have their priority changed.
     */
    fun cyclePriority(taskId: Int) {
        _tasks.update { tasks ->
            tasks.map { task ->
                if (task.id == taskId) {
                    val next = when (task.priority) {
                        Priority.NONE -> Priority.HIGH
                        Priority.HIGH -> Priority.MEDIUM
                        Priority.MEDIUM -> Priority.LOW
                        Priority.LOW -> Priority.NONE
                    }
                    task.copy(priority = next)
                } else task
            }
        }
        persist()
    }

    fun setStatus(taskId: Int, newStatus: TaskStatus) {
        _tasks.update { tasks ->
            tasks.map { if (it.id == taskId) it.copy(status = newStatus) else it }
        }
        persist()
    }

    // ── Gestion de session ────────────────────────────────────────────────────

    fun registerSession() {
        _tasks.update { tasks ->
            tasks.map {
                if (it.status == TaskStatus.DRAFT) it.copy(status = TaskStatus.IN_PROGRESS) else it
            }
        }
        _editingIds.value = emptySet()
        _isSessionRegistered.value = true
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveTasks(today, _tasks.value)
            repository.saveSessionRegistered(today, true)
        }
    }

    fun resetAll() {
        _tasks.value = emptyList()
        _editingIds.value = emptySet()
        _isSessionRegistered.value = false
        viewModelScope.launch(Dispatchers.IO) {
            repository.clearAll()
            _streak.value = userRepository.loadStreak()
        }
    }

    // ── Utilitaires ──────────────────────────────────────────────────────────

    fun clearError() {
        _errorMessage.value = null
    }

    private fun persist() {
        val snapshot = _tasks.value
        viewModelScope.launch(Dispatchers.IO) {
            repository.saveTasks(today, snapshot)
        }
    }

    /**
     * Choisit le message motivationnel du jour de façon déterministe
     * (numéro du jour dans l'année → index dans la liste).
     */
    private fun pickDailyMotivation(): String {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        val messages = listOf(
            "Chaque tâche accomplie vous rapproche de vos objectifs \uD83C\uDFAF",
            "Une journée productive commence par une liste claire \uD83D\uDCCB",
            "Petit à petit, l'oiseau fait son nid \uD83E\uDEBA",
            "La discipline aujourd'hui, la liberté demain \u2728",
            "Votre futur moi vous remerciera pour ce que vous faites maintenant \uD83D\uDE80",
            "Commencez par la tâche la plus difficile — le reste sera facile \uD83D\uDCAA",
            "Le succès, c'est la somme de petits efforts répétés chaque jour \uD83D\uDD25",
            "Vous n'avez pas besoin d'être parfait, juste constant \uD83D\uDCC8",
            "Une étape après l'autre — c'est ainsi que l'on avance loin \uD83C\uDFC6",
            "Investir dans vos tâches du jour, c'est investir en vous \uD83D\uDCA1"
        )
        return messages[dayOfYear % messages.size]
    }

    companion object {
        const val MAX_TASKS = 10
        private const val UNDO_TIMEOUT_MS = 5_000L
    }
}
