package com.example.mobiledev.ui.auth

import androidx.annotation.StringRes
import com.example.mobiledev.data.model.UserRole

data class AuthUiState(
    val isLoading: Boolean = false,
    @StringRes val errorRes: Int? = null,
    @StringRes val infoRes: Int? = null,
    val loggedInRole: UserRole? = null,   // zet navigatie in gang
    val isCheckingSession: Boolean = true // splash tijdens opstart-check
)