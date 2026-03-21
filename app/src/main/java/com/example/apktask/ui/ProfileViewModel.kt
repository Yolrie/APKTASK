package com.example.apktask.ui

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.apktask.data.UserRepository
import com.example.apktask.model.Streak
import com.example.apktask.model.UserProfile
import com.example.apktask.util.DateUtils
import com.example.apktask.util.InputValidator
import com.example.apktask.util.NotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel de l'onglet Profil.
 *
 * Gère :
 *  - Affichage et modification du profil (nom, avatar, visibilité)
 *  - Configuration des notifications
 *  - Génération du texte de partage de progression
 *  - Copie du code ami dans le presse-papiers
 *
 * Architecture StateFlow :
 *  - [profile] et [streak] : état persistant → StateFlow (valeur toujours disponible)
 *  - [successMessage] et [errorMessage] : événements one-shot → StateFlow<String?>
 *    consommés via clearMessages() depuis la vue après affichage
 */
class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository(application)

    // ── État interne (privé, mutable) ────────────────────────────────────────

    private val _profile = MutableStateFlow<UserProfile>(UserProfile())
    private val _streak = MutableStateFlow<Streak>(Streak())
    private val _successMessage = MutableStateFlow<String?>(null)
    private val _errorMessage = MutableStateFlow<String?>(null)

    // ── État exposé (lecture seule) ───────────────────────────────────────────

    val profile: StateFlow<UserProfile> = _profile.asStateFlow()
    val streak: StateFlow<Streak> = _streak.asStateFlow()
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            _profile.value = userRepository.loadProfile()
            _streak.value = userRepository.loadStreak()
        }
    }

    // ── Nom ──────────────────────────────────────────────────────────────────

    fun updateDisplayName(rawName: String) {
        when (val result = InputValidator.validateTitle(rawName)) {
            is InputValidator.Result.Failure -> _errorMessage.value = result.reason
            is InputValidator.Result.Success -> {
                val updated = _profile.value.copy(displayName = result.sanitized)
                _profile.value = updated
                viewModelScope.launch(Dispatchers.IO) { userRepository.saveProfile(updated) }
            }
        }
    }

    // ── Avatar ────────────────────────────────────────────────────────────────

    fun updateAvatarColor(index: Int) {
        if (index !in 0..7) return
        val updated = _profile.value.copy(avatarColorIndex = index)
        _profile.value = updated
        viewModelScope.launch(Dispatchers.IO) { userRepository.saveProfile(updated) }
    }

    // ── Visibilité ────────────────────────────────────────────────────────────

    fun togglePublic() {
        val updated = _profile.value.copy(isPublic = !_profile.value.isPublic)
        _profile.value = updated
        viewModelScope.launch(Dispatchers.IO) { userRepository.saveProfile(updated) }
    }

    // ── Notifications ─────────────────────────────────────────────────────────

    fun updateNotifications(
        morningEnabled: Boolean,
        eveningEnabled: Boolean,
        morningHour: Int,
        eveningHour: Int
    ) {
        val app = getApplication<Application>()
        val updated = _profile.value.copy(
            notifMorningEnabled = morningEnabled,
            notifEveningEnabled = eveningEnabled,
            notifMorningHour = morningHour,
            notifEveningHour = eveningHour
        )
        _profile.value = updated
        viewModelScope.launch(Dispatchers.IO) { userRepository.saveProfile(updated) }

        NotificationHelper.cancelAll(app)
        if (morningEnabled) NotificationHelper.scheduleMorning(app, morningHour)
        if (eveningEnabled) NotificationHelper.scheduleEvening(app, eveningHour)

        _successMessage.value = "Rappels mis à jour"
    }

    // ── Verrouillage biométrique ──────────────────────────────────────────────

    fun toggleBiometricLock() {
        val updated = _profile.value.copy(
            biometricLockEnabled = !_profile.value.biometricLockEnabled
        )
        _profile.value = updated
        viewModelScope.launch(Dispatchers.IO) { userRepository.saveProfile(updated) }
    }

    // ── Partage ───────────────────────────────────────────────────────────────

    /**
     * Génère un texte de partage de la progression du jour.
     * Les tâches individuelles NE sont PAS incluses pour préserver la vie privée.
     */
    fun buildShareText(completedToday: Int, totalToday: Int): String {
        val p = _profile.value
        val s = _streak.value
        val date = DateUtils.todayReadable()
        val name = p.displayName.ifBlank { "Moi" }
        return buildString {
            append("\uD83D\uDCCB $name sur Do.it — $date\n")
            append("\u2705 $completedToday/$totalToday tâches accomplies\n")
            if (s.count > 0) append("\uD83D\uDD25 Série : ${s.count} jour${if (s.count > 1) "s" else ""}\n")
            append("\nRejoignez-moi sur Do.it ! Mon code ami : ${p.friendCode}")
        }
    }

    /** Copie le code ami dans le presse-papiers. */
    fun copyFriendCode() {
        val code = _profile.value.friendCode
        val clipboard = getApplication<Application>()
            .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Code ami Do.it", code))
        _successMessage.value = "Code copié : $code"
    }

    fun clearMessages() {
        _successMessage.value = null
        _errorMessage.value = null
    }

    fun refreshStreak() {
        viewModelScope.launch(Dispatchers.IO) {
            _streak.value = userRepository.loadStreak()
        }
    }
}
