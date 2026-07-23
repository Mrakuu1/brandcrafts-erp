package com.brandcrafts.erp.feature.auth

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: LoginFieldError? = null,
    val passwordError: LoginFieldError? = null,
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
)

enum class LoginFieldError { EMAIL_REQUIRED, EMAIL_INVALID, PASSWORD_REQUIRED }
