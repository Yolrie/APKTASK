package com.example.apktask.util

import android.content.Context

/**
 * Garde rapide : stocke la dernière date d'injection des tâches récurrentes en SharedPreferences.
 * Évite tout appel Room si l'injection du jour est déjà faite (O(1) vs transaction DB).
 * La vérification définitive anti-doublon reste [LocalDataSource.loadInjectedRecurringTaskIds]
 * (couvre le cas Worker-minuit vs ouverture de l'app).
 */
object InjectionPrefs {

    private const val PREFS_NAME = "apktask_injection"
    private const val KEY_LAST_DATE = "last_injection_date"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getLastInjectionDate(context: Context): String =
        prefs(context).getString(KEY_LAST_DATE, "") ?: ""

    fun setLastInjectionDate(context: Context, date: String) {
        prefs(context).edit().putString(KEY_LAST_DATE, date).apply()
    }
}
