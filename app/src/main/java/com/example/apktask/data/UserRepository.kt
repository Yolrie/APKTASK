package com.example.apktask.data

import android.content.Context
import com.example.apktask.model.Streak
import com.example.apktask.model.Task
import com.example.apktask.model.TaskStatus
import com.example.apktask.model.UserProfile
import com.example.apktask.util.DateUtils

/**
 * Gère le profil utilisateur et la série (streak).
 *
 * Toutes les fonctions sont suspend et s'exécutent sur Dispatchers.IO.
 */
class UserRepository(context: Context) {
    private val local = LocalDataSource.getInstance(context)

    // ── Profil ───────────────────────────────────────────────────────────────

    suspend fun loadProfile(): UserProfile = local.loadProfile()

    suspend fun saveProfile(profile: UserProfile) {
        local.saveProfile(profile)
    }

    // ── Streak ───────────────────────────────────────────────────────────────

    suspend fun loadStreak(): Streak = local.loadStreak()

    /**
     * Évalue la journée [date] et met à jour la série.
     * À appeler à minuit, AVANT de vider les tâches.
     */
    suspend fun evaluateStreakForDay(date: String, tasks: List<Task>) {
        if (tasks.isEmpty()) return

        val current = local.loadStreak()
        val allDone = tasks.all { it.status == TaskStatus.COMPLETED }

        val newStreak = if (allDone) {
            if (current.lastCountedDate == date) {
                current // Déjà comptabilisé aujourd'hui
            } else {
                val consecutive = DateUtils.areConsecutiveDays(current.lastCountedDate, date)
                val newCount = if (consecutive) current.count + 1 else 1
                Streak(
                    count = newCount,
                    lastCountedDate = date,
                    longestEver = maxOf(current.longestEver, newCount)
                )
            }
        } else {
            Streak(count = 0, lastCountedDate = date, longestEver = current.longestEver)
        }

        local.saveStreak(newStreak)
    }

}
