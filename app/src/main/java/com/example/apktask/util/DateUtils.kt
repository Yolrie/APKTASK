package com.example.apktask.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Utilitaires de manipulation de dates pour Do.it.
 *
 * Toutes les dates sont au format ISO 8601 (YYYY-MM-DD) pour garantir
 * un tri lexicographique correct et éviter les ambiguïtés de locale.
 */
object DateUtils {

    private val ISO_FORMAT = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /** Date d'aujourd'hui : "2026-03-11" */
    fun today(): String = ISO_FORMAT.format(Date())

    /** Date d'hier : "2026-03-10" */
    fun yesterday(): String {
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        return ISO_FORMAT.format(cal.time)
    }

    /**
     * Date lisible en français : "Mercredi 11 mars 2026"
     * Utilisée dans les en-têtes de l'interface.
     */
    fun todayReadable(): String {
        val sdf = SimpleDateFormat("EEEE dd MMMM yyyy", Locale.FRENCH)
        return sdf.format(Date()).replaceFirstChar { it.uppercaseChar() }
    }

    /**
     * Vérifie si [earlier] et [later] sont des jours consécutifs.
     * Utilisé pour le calcul de la série (streak).
     */
    fun areConsecutiveDays(earlier: String, later: String): Boolean {
        if (earlier.isBlank() || later.isBlank()) return false
        return try {
            val d1 = ISO_FORMAT.parse(earlier) ?: return false
            val cal = Calendar.getInstance().apply { time = d1 }
            cal.add(Calendar.DAY_OF_YEAR, 1)
            ISO_FORMAT.format(cal.time) == later
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Retourne le [Calendar.DAY_OF_WEEK] pour une date ISO-8601, ou `null` si le format est invalide.
     * Utilisé par [com.example.apktask.model.RecurrenceRule.isDueOn].
     */
    fun dayOfWeekFor(date: String): Int? = try {
        val parsed = ISO_FORMAT.parse(date) ?: return null
        Calendar.getInstance().apply { time = parsed }.get(Calendar.DAY_OF_WEEK)
    } catch (_: Exception) {
        null
    }
}
