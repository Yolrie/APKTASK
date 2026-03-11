package com.example.apktask.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import com.example.apktask.data.TaskRepository
import com.example.apktask.model.Task
import com.example.apktask.model.TaskStatus
import com.example.apktask.util.InputValidator

/**
 * ViewModel MVVM — source de vérité unique pour l'UI.
 *
 * Responsabilités :
 *  - Orchestrer les opérations CRUD sur les tâches
 *  - Gérer l'état d'édition temporaire (UI uniquement)
 *  - Exposer des LiveData consommées par MainActivity
 *  - Persister via TaskRepository (données chiffrées)
 *
 * Sécurité :
 *  - Toute entrée utilisateur passe par InputValidator avant traitement
 *  - MAX_TASKS limite le nombre de tâches (anti-abus)
 *  - Aucune donnée sensible n'est tracée dans les logs
 */
class TaskViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = TaskRepository(application)

    // ── État interne ─────────────────────────────────────────────────────────

    private val _tasks = MutableLiveData<List<Task>>(emptyList())
    private val _editingIds = MutableLiveData<Set<Int>>(emptySet())

    val isSessionRegistered = MutableLiveData<Boolean>(false)

    /** Message d'erreur à afficher une seule fois (one-shot). */
    val errorMessage = MutableLiveData<String?>()

    // ── État UI combiné (tâches + édition) ───────────────────────────────────

    /**
     * Liste combinant l'état métier et l'état d'édition UI.
     * Utilisée directement par les adapters RecyclerView.
     */
    val tasksUiState: LiveData<List<TaskUiState>> =
        MediatorLiveData<List<TaskUiState>>().also { mediator ->
            mediator.addSource(_tasks) { tasks ->
                mediator.value = buildUiState(tasks, _editingIds.value.orEmpty())
            }
            mediator.addSource(_editingIds) { editingIds ->
                mediator.value = buildUiState(_tasks.value.orEmpty(), editingIds)
            }
        }

    private fun buildUiState(tasks: List<Task>, editingIds: Set<Int>): List<TaskUiState> =
        tasks.map { task -> TaskUiState(task = task, isEditing = task.id in editingIds) }

    // ── Initialisation ───────────────────────────────────────────────────────

    init {
        _tasks.value = repository.loadTasks()
        isSessionRegistered.value = repository.loadSessionRegistered()
    }

    // ── Opérations CRUD ──────────────────────────────────────────────────────

    fun addTask(title: String) {
        when (val result = InputValidator.validateTitle(title)) {
            is InputValidator.Result.Failure -> {
                errorMessage.value = result.reason
            }
            is InputValidator.Result.Success -> {
                val current = _tasks.value.orEmpty()
                val draftCount = current.count { it.status == TaskStatus.DRAFT }
                if (draftCount >= MAX_TASKS) {
                    errorMessage.value = "Maximum $MAX_TASKS tâches autorisées"
                    return
                }
                val newId = (current.maxOfOrNull { it.id } ?: 0) + 1
                _tasks.value = current + Task(id = newId, title = result.sanitized)
                persist()
            }
        }
    }

    fun startEditing(taskId: Int) {
        _editingIds.value = (_editingIds.value.orEmpty()) + taskId
    }

    fun cancelEditing(taskId: Int) {
        _editingIds.value = (_editingIds.value.orEmpty()) - taskId
    }

    fun saveEdit(taskId: Int, rawTitle: String) {
        when (val result = InputValidator.validateTitle(rawTitle)) {
            is InputValidator.Result.Failure -> {
                errorMessage.value = result.reason
            }
            is InputValidator.Result.Success -> {
                _tasks.value = _tasks.value.orEmpty().map { task ->
                    if (task.id == taskId) task.copy(title = result.sanitized) else task
                }
                _editingIds.value = (_editingIds.value.orEmpty()) - taskId
                persist()
            }
        }
    }

    fun deleteTask(taskId: Int) {
        _tasks.value = _tasks.value.orEmpty().filter { it.id != taskId }
        _editingIds.value = (_editingIds.value.orEmpty()) - taskId
        persist()
    }

    fun setStatus(taskId: Int, newStatus: TaskStatus) {
        _tasks.value = _tasks.value.orEmpty().map { task ->
            if (task.id == taskId) task.copy(status = newStatus) else task
        }
        persist()
    }

    // ── Gestion de session ───────────────────────────────────────────────────

    /** Valide la session : toutes les tâches DRAFT passent en IN_PROGRESS. */
    fun registerSession() {
        _tasks.value = _tasks.value.orEmpty().map { task ->
            if (task.status == TaskStatus.DRAFT) task.copy(status = TaskStatus.IN_PROGRESS)
            else task
        }
        _editingIds.value = emptySet()
        isSessionRegistered.value = true
        persist()
        repository.saveSessionRegistered(true)
    }

    /** Réinitialise toutes les données de la session courante. */
    fun resetAll() {
        repository.clearAll()
        _tasks.value = emptyList()
        _editingIds.value = emptySet()
        isSessionRegistered.value = false
    }

    // ── Utilitaires ──────────────────────────────────────────────────────────

    fun clearError() {
        errorMessage.value = null
    }

    private fun persist() {
        repository.saveTasks(_tasks.value.orEmpty())
    }

    companion object {
        private const val MAX_TASKS = 10
    }
}
