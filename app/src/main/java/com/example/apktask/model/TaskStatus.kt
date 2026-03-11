package com.example.apktask.model

/**
 * Représente les états possibles d'une tâche.
 *
 * DRAFT       (0) : Tâche créée mais pas encore enregistrée dans la session
 * IN_PROGRESS (1) : Session enregistrée, tâche active
 * COMPLETED   (2) : Tâche marquée comme terminée
 * CANCELLED   (3) : Tâche annulée
 */
enum class TaskStatus(val code: Int) {
    DRAFT(0),
    IN_PROGRESS(1),
    COMPLETED(2),
    CANCELLED(3);

    companion object {
        fun fromCode(code: Int): TaskStatus =
            entries.firstOrNull { it.code == code } ?: DRAFT
    }
}
