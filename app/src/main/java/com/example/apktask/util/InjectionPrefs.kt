package com.example.apktask.util

import android.content.Context

/**
 * Stockage léger de la dernière date d'injection des tâches récurrentes.
 *
 * **Pourquoi SharedPreferences et non Room ?**
 * La date d'injection n'est pas une donnée sensible (c'est une chaîne ISO-8601 du type
 * "2026-03-21"). Une lecture/écriture dans SharedPreferences est O(1) et évite
 * d'ouvrir une transaction Room uniquement pour cette vérification.
 *
 * **Double protection anti-doublon** :
 *  1. Ce garde rapide (SharedPrefs) : si [getLastInjectionDate] == aujourd'hui, on
 *     court-circuite toute la logique d'injection sans toucher à la base.
 *  2. [com.example.apktask.data.LocalDataSource.isRecurringTaskInjectedForDate]
 *     (requête Room) : vérification définitive par template, couvre le cas où le
 *     Worker minuit injecte avant l'ouverture de l'app (ou inversement).
 */
object InjectionPrefs {

    private const val PREFS_NAME = "apktask_injection"
    private const val KEY_LAST_DATE = "last_injection_date"

    fun getLastInjectionDate(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_LAST_DATE, "") ?: ""

    fun setLastInjectionDate(context: Context, date: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LAST_DATE, date).apply()
    }
}
