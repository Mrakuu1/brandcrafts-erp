package com.brandcrafts.erp.feature.employee

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.result.EmployeeError
import com.brandcrafts.erp.core.result.EmployeeResult
import com.brandcrafts.erp.core.validation.EmailValidator
import com.brandcrafts.erp.domain.model.Employee
import com.brandcrafts.erp.domain.model.EmployeeCreateCommand
import com.brandcrafts.erp.domain.model.EmployeeUpdateCommand
import com.brandcrafts.erp.domain.usecase.employee.CreateEmployeeUseCase
import com.brandcrafts.erp.domain.usecase.employee.GetEmployeeUseCase
import com.brandcrafts.erp.domain.usecase.employee.UpdateEmployeeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class EmployeeFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getEmployee: GetEmployeeUseCase,
    private val createEmployee: CreateEmployeeUseCase,
    private val updateEmployee: UpdateEmployeeUseCase,
) : ViewModel() {
    private val uid = savedStateHandle.get<String>(EMPLOYEE_ID_ARGUMENT)?.takeUnless { it == EMPTY_ID }
    private val mode = if (savedStateHandle.get<String>(MODE_ARGUMENT) == EDIT_MODE) EmployeeFormMode.EDIT else EmployeeFormMode.CREATE
    private val _uiState = MutableStateFlow(EmployeeFormUiState(mode = mode, uid = uid))
    val uiState: StateFlow<EmployeeFormUiState> = _uiState.asStateFlow()
    private val effectsChannel = Channel<EmployeeFormUiEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    init { if (mode == EmployeeFormMode.CREATE) readyForCreate() else uid?.let(::load) ?: loadFailed(EmployeeFormError.NOT_FOUND) }

    fun onEvent(event: EmployeeFormUiEvent) = when (event) {
        is EmployeeFormUiEvent.NameChanged -> update { copy(name = event.value, errors = errors.copy(name = null), saveError = null) }
        is EmployeeFormUiEvent.EmailChanged -> update { copy(email = event.value, errors = errors.copy(email = null), saveError = null) }
        is EmployeeFormUiEvent.PhoneChanged -> update { copy(phone = event.value, errors = errors.copy(phone = null), saveError = null) }
        is EmployeeFormUiEvent.RoleChanged -> update { copy(role = event.value, saveError = null) }
        is EmployeeFormUiEvent.ActiveChanged -> update { copy(active = event.value, saveError = null) }
        is EmployeeFormUiEvent.TemporaryPasswordChanged -> update { copy(temporaryPassword = event.value, errors = errors.copy(temporaryPassword = null), saveError = null) }
        EmployeeFormUiEvent.SaveClicked -> save()
        EmployeeFormUiEvent.CancelClicked -> emit(EmployeeFormUiEffect.NavigateBack)
        EmployeeFormUiEvent.RetryLoadClicked -> if (mode == EmployeeFormMode.CREATE) readyForCreate() else uid?.let(::load) ?: loadFailed(EmployeeFormError.NOT_FOUND)
    }

    private fun readyForCreate() { _uiState.value = EmployeeFormUiState(mode = EmployeeFormMode.CREATE, isInitialLoading = false) }
    private fun load(uid: String) {
        update { copy(isInitialLoading = true, loadError = null) }
        viewModelScope.launch { when (val result = getEmployee(uid)) {
            is EmployeeResult.Success -> _uiState.value = result.data.toUiState()
            is EmployeeResult.Error -> loadFailed(result.error.toFormError())
        } }
    }
    private fun save() {
        val state = _uiState.value
        if (state.isInitialLoading || state.isSaving || state.loadError != null) return
        val errors = state.validate()
        if (errors != EmployeeFormFieldErrors()) { update { copy(errors = errors, saveError = null) }; return }
        update { copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            val current = _uiState.value
            val result = if (current.mode == EmployeeFormMode.CREATE) {
                createEmployee(EmployeeCreateCommand(current.name.trim(), current.email.trim(), current.phone.trim(), current.role, current.active, current.temporaryPassword))
            } else {
                updateEmployee(EmployeeUpdateCommand(requireNotNull(current.uid), current.name.trim(), current.email.trim(), current.phone.trim(), current.role, current.active))
            }
            when (result) {
                is EmployeeResult.Success -> emit(EmployeeFormUiEffect.EmployeeSaved(current.mode))
                is EmployeeResult.Error -> update { copy(isSaving = false, saveError = result.error.toFormError()) }
            }
        }
    }
    private fun EmployeeFormUiState.validate() = EmployeeFormFieldErrors(
        name = if (name.trim().isBlank()) R.string.employee_form_name_required else null,
        email = if (!EmailValidator.isValid(email.trim())) R.string.login_email_invalid else null,
        phone = if (!phone.trim().isValidPhone()) R.string.employee_form_phone_invalid else null,
        temporaryPassword = if (mode == EmployeeFormMode.CREATE && temporaryPassword.length < 8) R.string.employee_form_password_invalid else null,
    )
    private fun String.isValidPhone(): Boolean { val digits = count(Char::isDigit); return digits in 7..15 && all { it.isDigit() || it == '+' || it == '-' || it == '(' || it == ')' || it.isWhitespace() } }
    private fun Employee.toUiState() = EmployeeFormUiState(EmployeeFormMode.EDIT, uid, name, email, phone, role, active, isInitialLoading = false)
    private fun EmployeeError.toFormError() = when (this) {
        EmployeeError.UNAUTHORIZED -> EmployeeFormError.UNAUTHORIZED; EmployeeError.EMPLOYEE_NOT_FOUND -> EmployeeFormError.NOT_FOUND
        EmployeeError.DUPLICATE_EMAIL -> EmployeeFormError.DUPLICATE_EMAIL; EmployeeError.DUPLICATE_PHONE -> EmployeeFormError.DUPLICATE_PHONE
        EmployeeError.NETWORK_UNAVAILABLE -> EmployeeFormError.NETWORK; EmployeeError.VALIDATION_FAILED -> EmployeeFormError.VALIDATION; EmployeeError.UNKNOWN -> EmployeeFormError.UNKNOWN
    }
    private fun loadFailed(error: EmployeeFormError) { update { copy(isInitialLoading = false, loadError = error) } }
    private fun update(transform: EmployeeFormUiState.() -> EmployeeFormUiState) { _uiState.value = _uiState.value.transform() }
    private fun emit(effect: EmployeeFormUiEffect) { viewModelScope.launch { effectsChannel.send(effect) } }
    private companion object { const val MODE_ARGUMENT = "mode"; const val EMPLOYEE_ID_ARGUMENT = "employeeId"; const val EDIT_MODE = "edit"; const val EMPTY_ID = "_" }
}
