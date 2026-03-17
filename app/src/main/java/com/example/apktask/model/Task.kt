package com.example.apktask.model

/**
 * Modèle de données immuable représentant une tâche.
 *
 * @param id        Identifiant unique positif
 * @param title     Titre sanitisé à la création
 * @param createdAt Timestamp de création (millisecondes)
 * @param status    État actuel de la tâche
 * @param date      Date de la tâche au format YYYY-MM-DD
 */
data class Task(
    val id: Int,
    val title: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: TaskStatus = TaskStatus.DRAFT,
    val date: String = ""
) {
    init {
        require(id > 0) { "L'identifiant de la tâche doit être positif" }
        require(title.isNotBlank()) { "Le titre de la tâche ne peut pas être vide" }
    }
}
