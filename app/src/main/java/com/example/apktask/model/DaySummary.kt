package com.example.apktask.model

/**
 * Bilan d'une journée de productivité.
 *
 * Créé à minuit par [MidnightResetWorker] avant le reset des tâches,
 * pour conserver un historique consultable dans l'onglet Historique.
 *
 * @param date              Date au format YYYY-MM-DD
 * @param totalTasks        Nombre total de tâches du jour
 * @param completedTasks    Tâches marquées COMPLETED
 * @param cancelledTasks    Tâches marquées CANCELLED
 * @param completionPercent Pourcentage d'accomplissement (0-100)
 * @param allDone           Vrai si toutes les tâches ont été accomplies
 * @param streakAtDay       Valeur de la série au moment de cette journée
 */
data class DaySummary(
    val date: String,
    val totalTasks: Int,
    val completedTasks: Int,
    val cancelledTasks: Int,
    val completionPercent: Int,
    val allDone: Boolean,
    val streakAtDay: Int
) {
    val inProgressTasks: Int
        get() = totalTasks - completedTasks - cancelledTasks

    val performanceEmoji: String
        get() = when {
            totalTasks == 0 -> "\uD83D\uDE34"       // rest day
            completionPercent == 100 -> "\uD83C\uDF1F" // perfect
            completionPercent >= 75 -> "\uD83D\uDCAA"  // strong
            completionPercent >= 50 -> "\uD83D\uDC4D"  // decent
            completionPercent >= 25 -> "\uD83D\uDE10"  // meh
            else -> "\uD83D\uDE15"                     // low
        }
}
