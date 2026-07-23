package com.brandcrafts.erp.feature.auth

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: ForgotPasswordEmailError? = null,
    val isLoading: Boolean = false,
    val isSuccess: Boolean = false,
)

enum class ForgotPasswordEmailError {
    REQUIRED,
    INVALID,
}
