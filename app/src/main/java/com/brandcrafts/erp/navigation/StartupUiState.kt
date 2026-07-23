package com.brandcrafts.erp.navigation

import com.brandcrafts.erp.core.result.AuthenticationError
sealed interface StartupUiState {
    data object Loading : StartupUiState
    data object Unauthenticated : StartupUiState
    data object Authenticated : StartupUiState
    data class RecoverableError(val error: AuthenticationError) : StartupUiState
}

sealed interface StartupUiEvent {
    data class LoginSucceeded(val user: com.brandcrafts.erp.domain.model.AuthenticatedUser) : StartupUiEvent
    data object RetryClicked : StartupUiEvent
    data object SignOutClicked : StartupUiEvent
}
