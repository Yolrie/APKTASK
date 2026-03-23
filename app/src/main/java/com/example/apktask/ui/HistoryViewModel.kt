package com.example.apktask.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.example.apktask.data.TaskRepository
import com.example.apktask.data.UserRepository
import com.example.apktask.model.DaySummary
import com.example.apktask.model.Streak
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * ViewModel de l'onglet Historique.
 *
 * Affiche les bilans journaliers passés et les statistiques globales de productivité.
 * Les données proviennent des [DaySummaryEntity] persistés chaque nuit par MidnightResetWorker.
 */
class HistoryViewModel(application: Application) : AndroidViewModel(application) {

    private val taskRepository = TaskRepository(application)
    private val userRepository = UserRepository(application)

    private val _summaries = MutableStateFlow<List<DaySummary>>(emptyList())
    private val _stats = MutableStateFlow(HistoryStats())
    private val _streak = MutableStateFlow(Streak())

    val summaries: StateFlow<List<DaySummary>> = _summaries.asStateFlow()
    val stats: StateFlow<HistoryStats> = _stats.asStateFlow()
    val streak: StateFlow<Streak> = _streak.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _summaries.value = taskRepository.loadRecentDaySummaries(30)
        _streak.value = userRepository.loadStreak()

        val totalDays = taskRepository.daySummaryCount()
        val avgPercent = if (totalDays > 0) taskRepository.averageCompletionPercent() else 0
        val perfectDays = taskRepository.perfectDaysCount()
        val totalTasks = _summaries.value.sumOf { it.totalTasks }
        val totalCompleted = _summaries.value.sumOf { it.completedTasks }

        _stats.value = HistoryStats(
            totalDaysTracked = totalDays,
            averageCompletion = avgPercent,
            perfectDays = perfectDays,
            totalTasksCreated = totalTasks,
            totalTasksCompleted = totalCompleted
        )
    }
}

/**
 * Statistiques globales d'historique affichées en haut de l'onglet.
 */
data class HistoryStats(
    val totalDaysTracked: Int = 0,
    val averageCompletion: Int = 0,
    val perfectDays: Int = 0,
    val totalTasksCreated: Int = 0,
    val totalTasksCompleted: Int = 0
)
