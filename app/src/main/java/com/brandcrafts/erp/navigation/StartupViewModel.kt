package com.brandcrafts.erp.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.core.result.AuthenticationError
import com.brandcrafts.erp.core.result.AppResult
import com.brandcrafts.erp.domain.usecase.GetCurrentUserUseCase
import com.brandcrafts.erp.domain.usecase.LogoutUseCase
import com.brandcrafts.erp.domain.usecase.ObserveCurrentUserUseCase
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
    private val observeCurrentUser: ObserveCurrentUserUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow<StartupUiState>(StartupUiState.Loading)
    val uiState: StateFlow<StartupUiState> = _uiState.asStateFlow()
    private val _logoutUiState = MutableStateFlow(LogoutUiState())
    val logoutUiState: StateFlow<LogoutUiState> = _logoutUiState.asStateFlow()
    val currentUser: StateFlow<CurrentUserState> = sessionManager.currentUser
    private var isSessionCheckInProgress = false
    private var sessionObservationJob: kotlinx.coroutines.Job? = null

    init {
        validateSession()
    }

    fun onEvent(event: StartupUiEvent) {
        when (event) {
            is StartupUiEvent.LoginSucceeded -> {
                sessionManager.setCurrentUser(event.user)
                _uiState.value = StartupUiState.Authenticated
                observeSession()
            }
            StartupUiEvent.RetryClicked -> validateSession()
            StartupUiEvent.SignOutClicked -> signOut()
            StartupUiEvent.LogoutConfirmed -> logoutFromProfile()
            StartupUiEvent.LogoutErrorShown -> _logoutUiState.value = _logoutUiState.value.copy(error = null)
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
                    observeSession()
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
        sessionObservationJob?.cancel()
        sessionManager.clearCurrentUser()
        _uiState.value = StartupUiState.Unauthenticated
        viewModelScope.launch {
            logout()
        }
    }

    private fun logoutFromProfile() {
        if (_logoutUiState.value.isLoading) return
        _logoutUiState.value = LogoutUiState(isLoading = true)
        viewModelScope.launch {
            when (val result = logout()) {
                is AppResult.Success -> {
                    sessionObservationJob?.cancel()
                    sessionManager.clearCurrentUser()
                    _logoutUiState.value = LogoutUiState()
                    _uiState.value = StartupUiState.Unauthenticated
                }
                is AppResult.Error -> {
                    _logoutUiState.value = LogoutUiState(error = result.error)
                }
            }
        }
    }

    private fun observeSession() {
        sessionObservationJob?.cancel()
        sessionObservationJob = viewModelScope.launch {
            observeCurrentUser().collect { result ->
                when (result) {
                    is AppResult.Success -> result.data?.let(sessionManager::setCurrentUser)
                    is AppResult.Error -> when (result.error) {
                        AuthenticationError.ACCOUNT_DISABLED,
                        AuthenticationError.USER_PROFILE_MISSING -> {
                            sessionManager.clearCurrentUser()
                            _uiState.value = StartupUiState.Unauthenticated
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}
