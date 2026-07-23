package com.brandcrafts.erp.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.core.result.AuthenticationError
import com.brandcrafts.erp.core.result.AppResult
import com.brandcrafts.erp.domain.usecase.GetCurrentUserUseCase
import com.brandcrafts.erp.domain.usecase.LogoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class StartupViewModel @Inject constructor(
    private val getCurrentUser: GetCurrentUserUseCase,
    private val logout: LogoutUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow<StartupUiState>(StartupUiState.Loading)
    val uiState: StateFlow<StartupUiState> = _uiState.asStateFlow()
    val currentUser: StateFlow<CurrentUserState> = sessionManager.currentUser
    private var isSessionCheckInProgress = false

    init {
        validateSession()
    }

    fun onEvent(event: StartupUiEvent) {
        when (event) {
            is StartupUiEvent.LoginSucceeded -> {
                sessionManager.setCurrentUser(event.user)
                _uiState.value = StartupUiState.Authenticated
            }
            StartupUiEvent.RetryClicked -> validateSession()
            StartupUiEvent.SignOutClicked -> signOut()
        }
    }

    private fun validateSession() {
        if (isSessionCheckInProgress) return
        isSessionCheckInProgress = true
        _uiState.value = StartupUiState.Loading
        viewModelScope.launch {
            _uiState.value = when (val result = getCurrentUser()) {
                is AppResult.Success -> result.data?.let {
                    sessionManager.setCurrentUser(it)
                    StartupUiState.Authenticated
                } ?: run {
                    sessionManager.clearCurrentUser()
                    StartupUiState.Unauthenticated
                }
                is AppResult.Error -> when (result.error) {
                    AuthenticationError.ACCOUNT_DISABLED,
                    AuthenticationError.USER_PROFILE_MISSING -> {
                        sessionManager.clearCurrentUser()
                        StartupUiState.Unauthenticated
                    }
                    else -> StartupUiState.RecoverableError(result.error)
                }
            }
            isSessionCheckInProgress = false
        }
    }

    private fun signOut() {
        sessionManager.clearCurrentUser()
        _uiState.value = StartupUiState.Unauthenticated
        viewModelScope.launch {
            logout()
        }
    }
}
