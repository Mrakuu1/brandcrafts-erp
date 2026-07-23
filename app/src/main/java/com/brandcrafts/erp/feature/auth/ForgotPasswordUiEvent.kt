package com.brandcrafts.erp.feature.auth

sealed interface ForgotPasswordUiEvent {
    data class EmailChanged(val value: String) : ForgotPasswordUiEvent
    data object SubmitClicked : ForgotPasswordUiEvent
    data object ReturnToLoginClicked : ForgotPasswordUiEvent
}
