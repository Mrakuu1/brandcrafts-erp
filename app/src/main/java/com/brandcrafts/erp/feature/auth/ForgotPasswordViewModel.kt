package com.brandcrafts.erp.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.core.result.AppResult
import com.brandcrafts.erp.core.validation.EmailValidator
import com.brandcrafts.erp.domain.usecase.ResetPasswordUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val resetPassword: ResetPasswordUseCase,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState.asStateFlow()
    private val effectsChannel = Channel<ForgotPasswordUiEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    fun onEvent(event: ForgotPasswordUiEvent) {
        when (event) {
            is ForgotPasswordUiEvent.EmailChanged -> {
                _uiState.value = _uiState.value.copy(email = event.value, emailError = null)
            }
            ForgotPasswordUiEvent.SubmitClicked -> submit()
            ForgotPasswordUiEvent.ReturnToLoginClicked -> {
                _uiState.value = ForgotPasswordUiState()
                viewModelScope.launch { effectsChannel.send(ForgotPasswordUiEffect.NavigateToLogin) }
            }
        }
    }

    private fun submit() {
        val state = _uiState.value
        if (state.isLoading || state.isSuccess) return
        val emailError = when {
            state.email.isBlank() -> ForgotPasswordEmailError.REQUIRED
            !EmailValidator.isValid(state.email) -> ForgotPasswordEmailError.INVALID
            else -> null
        }
        if (emailError != null) {
            _uiState.value = state.copy(emailError = emailError)
            return
        }
        _uiState.value = state.copy(isLoading = true)
        viewModelScope.launch {
            when (val result = resetPassword(state.email.trim())) {
                is AppResult.Success -> _uiState.value = ForgotPasswordUiState(isSuccess = true)
                is AppResult.Error -> {
                    _uiState.value = _uiState.value.copy(isLoading = false)
                    effectsChannel.send(ForgotPasswordUiEffect.ShowError(result.error))
                }
            }
        }
    }
}
