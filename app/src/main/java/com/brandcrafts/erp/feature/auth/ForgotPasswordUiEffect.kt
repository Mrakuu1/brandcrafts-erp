package com.brandcrafts.erp.feature.auth

import com.brandcrafts.erp.core.result.AuthenticationError

sealed interface ForgotPasswordUiEffect {
    data object NavigateToLogin : ForgotPasswordUiEffect
    data class ShowError(val error: AuthenticationError) : ForgotPasswordUiEffect
}
