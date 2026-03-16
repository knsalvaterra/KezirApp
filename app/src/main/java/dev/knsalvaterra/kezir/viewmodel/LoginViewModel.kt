package dev.knsalvaterra.kezir.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.knsalvaterra.kezir.api.AuthManager
import dev.knsalvaterra.kezir.api.LoginResult
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    fun performLogin(pin: String, eventId: String) {
        viewModelScope.launch {
            val result = AuthManager.login(pin, eventId)
            _loginResult.value = result
        }
    }

    fun getInMemorySession() = AuthManager.getInMemorySession()

    fun saveSessionInMemory(eventId: String, pin: String) {
        AuthManager.saveSessionInMemory(eventId, pin)
    }
}
