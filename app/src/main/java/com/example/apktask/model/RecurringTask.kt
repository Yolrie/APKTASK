package com.example.apktask.model

/**
 * Modèle domaine d'une tâche récurrente (template).
 *
 * Une [RecurringTask] n'est pas une tâche du jour — c'est un gabarit qui génère
 * automatiquement des [Task] chaque matin pour les jours correspondant à sa [rule].
 *
 * Cycle de vie :
 *  - [isActive] = true  → injectée chaque jour où [RecurrenceRule.isDueOn] retourne vrai
 *  - [isActive] = false → suspendue (pas de suppression, pour conserver l'historique)
 */
data class RecurringTask(
    val id: Int = 0,
    val title: String,
    val priority: Priority = Priority.NONE,
    val rule: RecurrenceRule,
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)
