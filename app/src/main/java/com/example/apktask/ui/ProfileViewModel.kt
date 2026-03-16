package com.example.apktask.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.example.apktask.data.UserRepository
import com.example.apktask.model.Streak
import com.example.apktask.model.UserProfile
import com.example.apktask.util.DateUtils
import com.example.apktask.util.InputValidator
import com.example.apktask.util.NotificationHelper

/**
 * ViewModel de l'onglet Profil.
 *
 * Gère :
 *  - Affichage et modification du profil (nom, avatar, visibilité)
 *  - Configuration des notifications
 *  - Génération du texte de partage de progression
 *  - Copie du code ami dans le presse-papiers
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository(application)

    val profile = MutableLiveData<UserProfile>(userRepository.loadProfile())
    val streak = MutableLiveData<Streak>(userRepository.loadStreak())
    val successMessage = MutableLiveData<String?>()
    val errorMessage = MutableLiveData<String?>()

    // ── Nom ──────────────────────────────────────────────────────────────────

    fun updateDisplayName(rawName: String) {
        when (val result = InputValidator.validateTitle(rawName)) {
            is InputValidator.Result.Failure -> errorMessage.value = result.reason
            is InputValidator.Result.Success -> {
                val updated = profile.value!!.copy(displayName = result.sanitized)
                profile.value = updated
                userRepository.saveProfile(updated)
            }
        }
    }

    // ── Avatar ────────────────────────────────────────────────────────────────

    fun updateAvatarColor(index: Int) {
        if (index !in 0..7) return
        val updated = profile.value!!.copy(avatarColorIndex = index)
        profile.value = updated
        userRepository.saveProfile(updated)
    }

    // ── Visibilité ────────────────────────────────────────────────────────────

    fun togglePublic() {
        val updated = profile.value!!.copy(isPublic = !profile.value!!.isPublic)
        profile.value = updated
        userRepository.saveProfile(updated)
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    fun updateNotifications(
        morningEnabled: Boolean,
        eveningEnabled: Boolean,
        morningHour: Int,
        eveningHour: Int
    ) {
        val app = getApplication<Application>()
        val updated = profile.value!!.copy(
            notifMorningEnabled = morningEnabled,
            notifEveningEnabled = eveningEnabled,
            notifMorningHour = morningHour,
            notifEveningHour = eveningHour
        )
        profile.value = updated
        userRepository.saveProfile(updated)

        NotificationHelper.cancelAll(app)
        if (morningEnabled) NotificationHelper.scheduleMorning(app, morningHour)
        if (eveningEnabled) NotificationHelper.scheduleEvening(app, eveningHour)

        successMessage.value = "Rappels mis à jour"
    }

    // ── Partage ───────────────────────────────────────────────────────────────

    /**
     * Génère un texte de partage de la progression du jour.
     * Les tâches individuelles NE sont PAS incluses pour préserver la vie privée.
     */
    fun buildShareText(completedToday: Int, totalToday: Int): String {
        val p = profile.value ?: return ""
        val s = streak.value ?: Streak()
        val date = DateUtils.todayReadable()
        val name = p.displayName.ifBlank { "Moi" }
        return buildString {
            append("📋 $name sur Do.it — $date\n")
            append("✅ $completedToday/$totalToday tâches accomplies\n")
            if (s.count > 0) append("🔥 Série : ${s.count} jour${if (s.count > 1) "s" else ""}\n")
            append("\nRejoignez-moi sur Do.it ! Mon code ami : ${p.friendCode}")
        }
    }

    /** Copie le code ami dans le presse-papiers. */
    fun copyFriendCode() {
        val code = profile.value?.friendCode ?: return
        val clipboard = getApplication<Application>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Code ami Do.it", code))
        successMessage.value = "Code copié : $code"
    }

    fun clearMessages() {
        successMessage.value = null
        errorMessage.value = null
    }

    fun refreshStreak() {
        streak.value = userRepository.loadStreak()
    }
}
