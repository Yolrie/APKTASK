package com.example.apktask.model

import java.util.UUID

/**
 * Profil de l'utilisateur local et social.
 *
 * @param userId              UUID auto-généré — sert aussi de code ami
 * @param displayName         Prénom/pseudo affiché aux amis
 * @param avatarColorIndex    Index (0..7) de la couleur d'avatar
 * @param isPublic            Rend la progression visible par les amis
 * @param notifMorningEnabled Rappel du matin activé
 * @param notifEveningEnabled Rappel du soir activé (si tâches non faites)
 * @param notifMorningHour    Heure du rappel matin (0–23)
 * @param notifEveningHour    Heure du rappel soir (0–23)
 *
 * Sécurité : stocké chiffré dans LocalDataSource. Le userId n'est jamais
 * loggué ni exposé dans les notifications.
 */
data class UserProfile(
    val userId: String = UUID.randomUUID().toString(),
    val displayName: String = "",
    val avatarColorIndex: Int = 0,
    val isPublic: Boolean = false,
    val notifMorningEnabled: Boolean = true,
    val notifEveningEnabled: Boolean = true,
    val notifMorningHour: Int = 8,
    val notifEveningHour: Int = 21,
    val biometricLockEnabled: Boolean = false
) {
    /** Lettre de l'avatar (1ère lettre du nom, ou "?" si vide). */
    val avatarLetter: String
        get() = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?"

    /**
     * Code ami partageable : 8 premiers caractères du userId en majuscules.
     * Exemple : "A1B2C3D4"
     * Assez court pour être dicté ou copié, assez unique pour identifier un user.
     */
    val friendCode: String
        get() = userId.take(8).uppercase()
}
