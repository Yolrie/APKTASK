package com.example.apktask.data.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.apktask.model.Priority
import com.example.apktask.model.Task
import com.example.apktask.model.TaskStatus

/**
 * Entité Room représentant une tâche persistée.
 *
 * Index sur [date] : la requête principale filtre toujours par date (SELECT WHERE date = ?),
 * l'index évite un full scan de la table même si elle grandit au fil des semaines.
 *
 * [status] et [priority] sont stockés en Int (code numérique) plutôt qu'en String pour
 * limiter la surface de désérialisation — un code entier ne peut pas transporter de payload.
 */
@Entity(
    tableName = "tasks",
    indices = [Index(value = ["date"])]
)
data class TaskEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val createdAt: Long,
    val status: Int,
    val date: String,
    val priority: Int = 0,  // Priority.NONE — ajouté en v2 via migration
    @ColumnInfo(name = "recurring_task_id") val recurringTaskId: Int? = null  // null = tâche manuelle
) {
    fun toTask() = Task(
        id = id,
        title = title,
        createdAt = createdAt,
        status = TaskStatus.fromCode(status),
        date = date,
        priority = Priority.fromCode(priority),
        recurringTaskId = recurringTaskId
    )
}

fun Task.toEntity() = TaskEntity(
    id = id,
    title = title,
    createdAt = createdAt,
    status = status.code,
    date = date,
    priority = priority.code,
    recurringTaskId = recurringTaskId
)
