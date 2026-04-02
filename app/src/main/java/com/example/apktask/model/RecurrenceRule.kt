package com.example.apktask.model

import java.util.Calendar
import com.example.apktask.util.DateUtils

/**
 * Fréquence de répétition d'une tâche récurrente.
 *
 *  - DAILY    : chaque jour sans exception
 *  - WEEKDAYS : du lundi au vendredi uniquement
 *  - WEEKLY   : un seul jour de la semaine (stocké via [RecurrenceRule.daysBitmask])
 *  - CUSTOM   : plusieurs jours choisis (bitmask multi-bits)
 */
enum class Frequency(val code: Int, val label: String) {
    DAILY(0, "Chaque jour"),
    WEEKDAYS(1, "Jours ouvrés"),
    WEEKLY(2, "Hebdomadaire"),
    CUSTOM(3, "Personnalisé");

    companion object {
        fun fromCode(code: Int): Frequency = entries.firstOrNull { it.code == code } ?: DAILY
    }
}

/**
 * Règle de récurrence associée à une [RecurringTask].
 *
 * **Encodage du bitmask** :
 * Le bit à la position [Calendar.DAY_OF_WEEK] est activé si ce jour est inclus.
 * Ex. : lundi + mercredi = (1 shl Calendar.MONDAY) or (1 shl Calendar.WEDNESDAY) = 0b0000110
 *
 * Pour [Frequency.DAILY] et [Frequency.WEEKDAYS] le bitmask est ignoré.
 * Pour [Frequency.WEEKLY] un seul bit est attendu.
 * Pour [Frequency.CUSTOM] plusieurs bits peuvent être activés.
 *
 * **Factories** :
 *  - [daily]    → tous les jours
 *  - [weekdays] → lun–ven
 *  - [weekly]   → un jour précis (ex. Calendar.MONDAY)
 *  - [custom]   → liste de jours (ex. Calendar.MONDAY, Calendar.WEDNESDAY)
 */
data class RecurrenceRule(
    val frequency: Frequency,
    /** Bitmask des jours Calendar.DAY_OF_WEEK actifs (utilisé pour WEEKLY et CUSTOM). */
    val daysBitmask: Int = 0
) {

    /**
     * Retourne `true` si cette règle est satisfaite pour la date [date] au format ISO-8601 (yyyy-MM-dd).
     */
    fun isDueOn(date: String): Boolean {
        val dayOfWeek = DateUtils.dayOfWeekFor(date) ?: return false
        return when (frequency) {
            Frequency.DAILY -> true
            Frequency.WEEKDAYS -> dayOfWeek in Calendar.MONDAY..Calendar.FRIDAY
            Frequency.WEEKLY, Frequency.CUSTOM -> daysBitmask and (1 shl dayOfWeek) != 0
        }
    }

    companion object {
        fun daily() = RecurrenceRule(Frequency.DAILY)

        fun weekdays() = RecurrenceRule(Frequency.WEEKDAYS)

        /** @param dayOfWeek valeur [Calendar.DAY_OF_WEEK] (ex. [Calendar.MONDAY]) */
        fun weekly(dayOfWeek: Int) = RecurrenceRule(Frequency.WEEKLY, 1 shl dayOfWeek)

        /** @param daysOfWeek valeurs [Calendar.DAY_OF_WEEK] (ex. [Calendar.MONDAY], [Calendar.WEDNESDAY]) */
        fun custom(vararg daysOfWeek: Int) =
            RecurrenceRule(Frequency.CUSTOM, daysOfWeek.fold(0) { mask, day -> mask or (1 shl day) })
    }
}
