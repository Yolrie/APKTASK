package com.example.apktask.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.apktask.data.UserRepository
import com.example.apktask.model.FriendProgress
import com.example.apktask.util.DateUtils
import kotlinx.coroutines.launch

/**
 * ViewModel de l'onglet Social.
 *
 * Gère la liste des amis et leur progression journalière.
 * En mode mock (sans Firebase) : affiche des amis de démonstration.
 * En mode Firebase : affiche les vrais amis avec leur progression réelle.
 */
class SocialViewModel(application: Application) : AndroidViewModel(application) {

    private val userRepository = UserRepository(application)

    val friends = MutableLiveData<List<FriendProgress>>(emptyList())
    val isLoading = MutableLiveData<Boolean>(false)
    val errorMessage = MutableLiveData<String?>()
    val successMessage = MutableLiveData<String?>()

    init {
        loadFriends()
    }

    fun loadFriends() {
        isLoading.value = true
        viewModelScope.launch {
            val result = userRepository.getFriendsProgress(DateUtils.today())
            friends.value = result
            isLoading.value = false
        }
    }

    /**
     * Ajoute un ami via son code (8 caractères).
     * Retourne une erreur explicite si Firebase n'est pas configuré.
     */
    fun addFriend(rawCode: String) {
        val code = rawCode.trim().uppercase()
        if (code.length != 8) {
            errorMessage.value = "Le code ami doit contenir 8 caractères"
            return
        }
        viewModelScope.launch {
            isLoading.value = true
            userRepository.addFriendByCode(code)
                .onSuccess { friend ->
                    successMessage.value = "${friend.displayName} ajouté(e) !"
                    loadFriends()
                }
                .onFailure { error ->
                    errorMessage.value = error.message
                }
            isLoading.value = false
        }
    }

    fun removeFriend(userId: String) {
        userRepository.removeFriend(userId)
        friends.value = friends.value.orEmpty().filter { it.userId != userId }
    }

    fun clearMessages() {
        errorMessage.value = null
        successMessage.value = null
    }
}
