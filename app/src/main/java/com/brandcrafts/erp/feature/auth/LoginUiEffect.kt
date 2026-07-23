package com.brandcrafts.erp.feature.auth

import com.brandcrafts.erp.core.result.AuthenticationError
import com.brandcrafts.erp.domain.model.AuthenticatedUser

sealed interface LoginUiEffect {
    data class NavigateToMainShell(val user: AuthenticatedUser) : LoginUiEffect
    data class ShowError(val error: AuthenticationError) : LoginUiEffect
}
