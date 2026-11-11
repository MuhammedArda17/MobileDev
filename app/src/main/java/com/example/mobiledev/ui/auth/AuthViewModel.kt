package com.example.mobiledev.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.mobiledev.R
import com.example.mobiledev.data.model.UserRole
import com.example.mobiledev.data.repository.AuthError
import com.example.mobiledev.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val repo: AuthRepository = AuthRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        checkSession()
    }

    private fun checkSession() = viewModelScope.launch {
        try {
            val role = repo.getCurrentRole()
            _state.update { it.copy(loggedInRole = role, isCheckingSession = false) }
        } catch (e: AuthError) {
            // bv. geen internet bij opstart: toon loginscherm
            _state.update { it.copy(isCheckingSession = false) }
        }
    }

    fun login(email: String, password: String) {
        if (!validateEmail(email)) return
        if (password.isBlank()) return setError(R.string.error_password_empty)

        launchAuth { repo.login(email.trim(), password) }
    }

    fun register(name: String, email: String, password: String, role: UserRole) {
        if (name.isBlank()) return setError(R.string.error_name_empty)
        if (!validateEmail(email)) return
        if (password.length < 6) return setError(R.string.error_password_short)

        launchAuth { repo.register(name.trim(), email.trim(), password, role) }
    }

    fun resetPassword(email: String) {
        if (!validateEmail(email)) return
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, errorRes = null, infoRes = null) }
            try {
                repo.sendPasswordReset(email.trim())
                _state.update { it.copy(isLoading = false, infoRes = R.string.info_reset_sent) }
            } catch (e: AuthError) {
                _state.update { it.copy(isLoading = false, errorRes = e.toStringRes()) }
            }
        }
    }

    fun logout() {
        repo.logout()
        _state.update { AuthUiState(isCheckingSession = false) }
    }

    fun clearMessages() = _state.update { it.copy(errorRes = null, infoRes = null) }

    private fun launchAuth(block: suspend () -> UserRole) = viewModelScope.launch {
        _state.update { it.copy(isLoading = true, errorRes = null) }
        try {
            val role = block()
            _state.update { it.copy(isLoading = false, loggedInRole = role) }
        } catch (e: AuthError) {
            _state.update { it.copy(isLoading = false, errorRes = e.toStringRes()) }
        }
    }

    private fun validateEmail(email: String): Boolean {
        val ok = Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
        if (!ok) setError(R.string.error_email_invalid)
        return ok
    }

    private fun setError(res: Int) = _state.update { it.copy(errorRes = res) }

    private fun AuthError.toStringRes(): Int = when (this) {
        AuthError.InvalidCredentials -> R.string.error_invalid_credentials
        AuthError.EmailInUse -> R.string.error_email_in_use
        AuthError.WeakPassword -> R.string.error_password_short
        AuthError.Network -> R.string.error_no_internet
        AuthError.UserNotFound -> R.string.error_invalid_credentials
        AuthError.ProfileMissing -> R.string.error_profile_missing
        AuthError.Unknown -> R.string.error_unknown
    }
}