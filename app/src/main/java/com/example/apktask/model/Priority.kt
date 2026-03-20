package com.example.apktask.model

/**
 * Niveau de priorité d'une tâche.
 *
 * Stocké en Int dans Room (code) pour limiter la surface de désérialisation.
 * L'ordre NONE < LOW < MEDIUM < HIGH permet un tri décroissant par code.
 *
 * Cycle d'affichage : appui sur le chip cycle NONE → HIGH → MEDIUM → LOW → NONE.
 */
enum class Priority(val code: Int, val label: String, val emoji: String) {
    NONE(0, "—", ""),
    LOW(1, "Bas", "🔵"),
    MEDIUM(2, "Moyen", "🟡"),
    HIGH(3, "Haute", "🔴");

    companion object {
        fun fromCode(code: Int): Priority =
            entries.firstOrNull { it.code == code } ?: NONE
    }
}
