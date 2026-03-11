package com.example.apktask.ui

import com.example.apktask.model.Task

/**
 * Enveloppe un [Task] avec son état d'édition UI.
 *
 * Séparation claire entre l'état métier persisté (Task) et l'état UI
 * transitoire (isEditing). DiffUtil compare les deux champs, ce qui
 * garantit un rebind précis sans appel à notifyDataSetChanged().
 */
data class TaskUiState(
    val task: Task,
    val isEditing: Boolean = false
)
