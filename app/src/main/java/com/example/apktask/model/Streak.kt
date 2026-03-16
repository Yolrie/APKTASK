package com.example.apktask.model

/**
 * Série de jours consécutifs où 100 % des tâches ont été accomplies.
 *
 * @param count           Jours consécutifs actuels
 * @param lastCountedDate Dernière date comptabilisée (YYYY-MM-DD)
 * @param longestEver     Record personnel de la série
 *
 * Règles :
 *  - S'incrémente à minuit si toutes les tâches de la journée sont COMPLETED
 *  - Se remet à 0 si des tâches restent non accomplies à minuit
 *  - Pas de changement si aucune tâche n'a été créée (jour de repos autorisé)
 */
data class Streak(
    val count: Int = 0,
    val lastCountedDate: String = "",
    val longestEver: Int = 0
) {
    /** Icône motivationnelle selon le niveau de la série. */
    val badge: String
        get() = when {
            count >= 30 -> "🔥🔥🔥"
            count >= 14 -> "🔥🔥"
            count >= 7  -> "🔥"
            count >= 3  -> "⚡"
            count >= 1  -> "✨"
            else        -> ""
        }
}
