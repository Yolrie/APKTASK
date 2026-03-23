package com.example.apktask.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.apktask.model.DaySummary

/**
 * Entité Room représentant le bilan d'une journée.
 *
 * Persisté juste AVANT le reset de minuit (MidnightResetWorker) pour conserver
 * l'historique de productivité une fois les tâches du jour effacées.
 *
 * Index sur [date] : les requêtes d'historique trient/filtrent par date.
 */
@Entity(
    tableName = "day_summaries",
    indices = [Index(value = ["date"], unique = true)]
)
data class DaySummaryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String,
    @ColumnInfo(name = "total_tasks") val totalTasks: Int,
    @ColumnInfo(name = "completed_tasks") val completedTasks: Int,
    @ColumnInfo(name = "cancelled_tasks") val cancelledTasks: Int,
    @ColumnInfo(name = "completion_percent") val completionPercent: Int,
    @ColumnInfo(name = "all_done") val allDone: Boolean,
    @ColumnInfo(name = "streak_at_day") val streakAtDay: Int
) {
    fun toDaySummary() = DaySummary(
        date = date,
        totalTasks = totalTasks,
        completedTasks = completedTasks,
        cancelledTasks = cancelledTasks,
        completionPercent = completionPercent,
        allDone = allDone,
        streakAtDay = streakAtDay
    )
}

fun DaySummary.toEntity() = DaySummaryEntity(
    date = date,
    totalTasks = totalTasks,
    completedTasks = completedTasks,
    cancelledTasks = cancelledTasks,
    completionPercent = completionPercent,
    allDone = allDone,
    streakAtDay = streakAtDay
)
