package com.example.apktask.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.apktask.data.UserRepository
import com.example.apktask.model.FriendProgress
import com.example.apktask.util.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * ViewModel de l'onglet Social.
 *
 * Gère la liste des amis et leur progression journalière.
 * En mode mock (sans Firebase) : affiche des amis de démonstration.
 * En mode Firebase : affiche les vrais amis avec leur progression réelle.
 *
 * Architecture StateFlow :
 *  - [friends], [isLoading] : état UI stable → StateFlow
 *  - [errorMessage], [successMessage] : événements one-shot → StateFlow<String?>
 *    consommés via clearMessages() depuis la vue après affichage
 *  - Les coroutines [viewModelScope.launch] restent inchangées : StateFlow est
 *    thread-safe et peut être mis à jour depuis n'importe quel dispatcher
 */
class SocialViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository(application)

    // ── État interne (privé, mutable) ────────────────────────────────────────

    private val _friends = MutableStateFlow<List<FriendProgress>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessage = MutableStateFlow<String?>(null)
    private val _successMessage = MutableStateFlow<String?>(null)

    // ── État exposé (lecture seule) ───────────────────────────────────────────

    val friends: StateFlow<List<FriendProgress>> = _friends.asStateFlow()
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    val successMessage: StateFlow<String?> = _successMessage.asStateFlow()

    init {
        loadFriends()
    }

    fun loadFriends() {
        _isLoading.value = true
        viewModelScope.launch {
            val result = userRepository.getFriendsProgress(DateUtils.today())
            _friends.value = result
            _isLoading.value = false
        }
    }

    /**
     * Ajoute un ami via son code (8 caractères).
     * Retourne une erreur explicite si Firebase n'est pas configuré.
     */
    fun addFriend(rawCode: String) {
        val code = rawCode.trim().uppercase()
        if (code.length != 8) {
            _errorMessage.value = "Le code ami doit contenir 8 caractères"
            return
        }
        viewModelScope.launch {
            _isLoading.value = true
            userRepository.addFriendByCode(code)
                .onSuccess { friend ->
                    _successMessage.value = "${friend.displayName} ajouté(e) !"
                    loadFriends()
                }
                .onFailure { error ->
                    _errorMessage.value = error.message
                }
            _isLoading.value = false
        }
    }

    fun removeFriend(userId: String) {
        _friends.update { friends -> friends.filter { it.userId != userId } }
        viewModelScope.launch(Dispatchers.IO) { userRepository.removeFriend(userId) }
    }

    fun clearMessages() {
        _errorMessage.value = null
        _successMessage.value = null
    }
}
