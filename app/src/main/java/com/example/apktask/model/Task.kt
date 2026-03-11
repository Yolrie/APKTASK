package com.example.apktask.model

/**
 * Modèle de données immuable représentant une tâche.
 *
 * @param id        Identifiant unique (positif, non nul)
 * @param title     Titre de la tâche (sanitisé à la création)
 * @param createdAt Timestamp de création en millisecondes
 * @param status    État actuel de la tâche
 */
data class Task(
    val id: Int,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: TaskStatus = TaskStatus.DRAFT
) {
    init {
        require(id > 0) { "L'identifiant de la tâche doit être positif" }
        require(title.isNotBlank()) { "Le titre de la tâche ne peut pas être vide" }
    }
}
