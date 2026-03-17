package com.example.apktask.model

/**
 * Résumé journalier de la progression d'un ami.
 *
 * @param userId          Identifiant de l'ami
 * @param displayName     Prénom/pseudo
 * @param avatarColorIndex Couleur d'avatar (0..7)
 * @param todayCompleted  Tâches terminées aujourd'hui
 * @param todayTotal      Total de tâches prévues aujourd'hui
 * @param streak          Série de jours consécutifs de l'ami
 * @param isMock          Données de démonstration (mode sans Firebase)
 *
 * Sécurité : seuls les profils marqués isPublic=true sont visibles.
 * Aucune tâche individuelle de l'ami n'est exposée, uniquement les compteurs.
 */
data class FriendProgress(
    val userId: String,
    val displayName: String,
    val avatarColorIndex: Int = 0,
    val todayCompleted: Int = 0,
    val todayTotal: Int = 0,
    val streak: Int = 0,
    val isMock: Boolean = false
) {
    val avatarLetter: String
        get() = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    val completionPercent: Int
        get() = if (todayTotal > 0) (todayCompleted * 100) / todayTotal else 0

    val isAllDone: Boolean
        get() = todayTotal > 0 && todayCompleted >= todayTotal
}
