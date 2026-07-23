package com.brandcrafts.erp.feature.auth

sealed interface LoginUiEvent {
    data class EmailChanged(val value: String) : LoginUiEvent
    data class PasswordChanged(val value: String) : LoginUiEvent
    data object PasswordVisibilityToggled : LoginUiEvent
    data object SignInClicked : LoginUiEvent
}
