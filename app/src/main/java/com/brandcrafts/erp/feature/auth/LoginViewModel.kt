package com.brandcrafts.erp.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.core.validation.EmailValidator
import com.brandcrafts.erp.core.result.AppResult
import com.brandcrafts.erp.domain.usecase.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()
    private val effectsChannel = Channel<LoginUiEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    fun onEvent(event: LoginUiEvent) {
        when (event) {
            is LoginUiEvent.EmailChanged -> _uiState.value = _uiState.value.copy(email = event.value, emailError = null)
            is LoginUiEvent.PasswordChanged -> _uiState.value = _uiState.value.copy(password = event.value, passwordError = null)
            LoginUiEvent.PasswordVisibilityToggled -> _uiState.value = _uiState.value.copy(isPasswordVisible = !_uiState.value.isPasswordVisible)
            LoginUiEvent.SignInClicked -> signIn()
        }
    }

    private fun signIn() {
        val state = _uiState.value
        if (state.isLoading) return
        val emailError = when {
            state.email.isBlank() -> LoginFieldError.EMAIL_REQUIRED
            !EmailValidator.isValid(state.email) -> LoginFieldError.EMAIL_INVALID
            else -> null
        }
        val passwordError = if (state.password.isBlank()) LoginFieldError.PASSWORD_REQUIRED else null
        if (emailError != null || passwordError != null) {
            _uiState.value = state.copy(emailError = emailError, passwordError = passwordError)
            return
        }
        _uiState.value = state.copy(isLoading = true)
        viewModelScope.launch {
            when (val result = loginUseCase(state.email.trim(), state.password)) {
                is AppResult.Success -> effectsChannel.send(LoginUiEffect.NavigateToMainShell(result.data))
                is AppResult.Error -> effectsChannel.send(LoginUiEffect.ShowError(result.error))
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}
