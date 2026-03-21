package com.example.apktask.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.apktask.model.Frequency
import com.example.apktask.model.Priority
import com.example.apktask.model.RecurrenceRule
import com.example.apktask.model.RecurringTask

/**
 * Entité Room pour les tâches récurrentes (table `recurring_tasks`).
 *
 * La [RecurrenceRule] est aplatie en deux colonnes :
 *  - [frequencyCode] → [Frequency.code]
 *  - [daysBitmask]   → bitmask Calendar.DAY_OF_WEEK (voir [RecurrenceRule])
 */
@Entity(tableName = "recurring_tasks")
data class RecurringTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val priority: Int = 0,
    @ColumnInfo(name = "frequency_code") val frequencyCode: Int,
    @ColumnInfo(name = "days_bitmask") val daysBitmask: Int = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_active") val isActive: Boolean = true
) {
    fun toRecurringTask() = RecurringTask(
        id = id,
        title = title,
        priority = Priority.fromCode(priority),
        rule = RecurrenceRule(Frequency.fromCode(frequencyCode), daysBitmask),
        createdAt = createdAt,
        isActive = isActive
    )
}

fun RecurringTask.toEntity() = RecurringTaskEntity(
    id = id,
    title = title,
    priority = priority.code,
    frequencyCode = rule.frequency.code,
    daysBitmask = rule.daysBitmask,
    createdAt = createdAt,
    isActive = isActive
)
