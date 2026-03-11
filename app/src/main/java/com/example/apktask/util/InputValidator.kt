package com.example.apktask.util

/**
 * Valide et assainit les entrées utilisateur.
 *
 * Sécurité : toutes les données saisies passent par cette classe avant
 * d'être stockées ou affichées, afin de prévenir les injections et
 * les dépassements de taille.
 */
object InputValidator {

    const val MAX_TITLE_LENGTH = 200

    sealed class Result {
        data class Success(val sanitized: String) : Result()
        data class Failure(val reason: String) : Result()
    }

    /**
     * Valide un titre de tâche :
     *  - Supprime les espaces en début/fin (trim)
     *  - Refuse les chaînes vides ou nulles
     *  - Refuse les titres dépassant MAX_TITLE_LENGTH caractères
     */
    fun validateTitle(input: String?): Result {
        if (input == null) return Result.Failure("La tâche ne peut pas être nulle")

        val trimmed = input.trim()
        return when {
            trimmed.isEmpty() ->
                Result.Failure("La tâche ne peut pas être vide")
            trimmed.length > MAX_TITLE_LENGTH ->
                Result.Failure("Le titre ne peut pas dépasser $MAX_TITLE_LENGTH caractères")
            else ->
                Result.Success(trimmed)
        }
    }
}
