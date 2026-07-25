package com.brandcrafts.erp.feature.employee

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.core.result.EmployeeError
import com.brandcrafts.erp.core.result.EmployeeResult
import com.brandcrafts.erp.domain.model.Employee
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.domain.usecase.employee.ObserveEmployeesUseCase
import com.brandcrafts.erp.domain.usecase.employee.SetEmployeeActiveUseCase
import com.brandcrafts.erp.domain.usecase.employee.UpdateEmployeeRoleUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import java.text.DateFormat
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class EmployeeManagementViewModel @Inject constructor(
    private val observeEmployees: ObserveEmployeesUseCase,
    private val setEmployeeActive: SetEmployeeActiveUseCase,
    private val updateEmployeeRole: UpdateEmployeeRoleUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val _uiState = MutableStateFlow(EmployeeManagementUiState())
    val uiState: StateFlow<EmployeeManagementUiState> = _uiState.asStateFlow()

    private val effectsChannel = Channel<EmployeeManagementUiEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()
    private var observationJob: Job? = null

    init {
        if (isActiveAdmin()) observe() else unauthorized()
    }

    fun onEvent(event: EmployeeManagementUiEvent) {
        when (event) {
            is EmployeeManagementUiEvent.SearchChanged -> {
                _uiState.value = _uiState.value.copy(searchQuery = event.value)
                filter()
            }
            is EmployeeManagementUiEvent.StatusChangeRequested -> requestStatusChange(event.employee)
            EmployeeManagementUiEvent.StatusChangeConfirmed -> changeStatus()
            EmployeeManagementUiEvent.StatusChangeDismissed -> _uiState.value = _uiState.value.copy(pendingStatusChange = null)
            EmployeeManagementUiEvent.AddEmployeeClicked -> if (isActiveAdmin()) {
                sendEffect(EmployeeManagementUiEffect.OpenEmployeeCreate)
            } else unauthorized()
            is EmployeeManagementUiEvent.EmployeeClicked -> _uiState.value = _uiState.value.copy(
                selectedEmployee = event.employee,
                selectedRole = event.employee.role,
            )
            is EmployeeManagementUiEvent.RoleSelected -> _uiState.value = _uiState.value.copy(selectedRole = event.role)
            EmployeeManagementUiEvent.RoleChangeConfirmed -> changeRole()
            EmployeeManagementUiEvent.EditEmployeeClicked -> _uiState.value.selectedEmployee?.let {
                sendEffect(EmployeeManagementUiEffect.OpenEmployeeEdit(it.uid))
            }
            EmployeeManagementUiEvent.EmployeeActionsDismissed -> _uiState.value = _uiState.value.copy(
                selectedEmployee = null,
                selectedRole = null,
            )
            EmployeeManagementUiEvent.RetryClicked -> if (isActiveAdmin()) observe() else unauthorized()
        }
    }

    private fun observe() {
        observationJob?.cancel()
        _uiState.value = _uiState.value.copy(content = EmployeeManagementUiState.Content.Loading)
        observationJob = viewModelScope.launch {
            observeEmployees().collect { result ->
                when (result) {
                    is EmployeeResult.Success -> {
                        val items = result.data.map { employee -> employee.toListItemUi() }
                        _uiState.value = _uiState.value.copy(allEmployees = items, updatingEmployeeUid = null)
                        filter()
                    }
                    is EmployeeResult.Error -> {
                        if (result.error == EmployeeError.UNAUTHORIZED) unauthorized()
                        else _uiState.value = _uiState.value.copy(content = EmployeeManagementUiState.Content.Error)
                    }
                }
            }
        }
    }

    private fun filter() {
        val state = _uiState.value
        val query = state.searchQuery.trim()
        val visible = state.allEmployees.filter { employee ->
            query.isBlank() || listOf(employee.name, employee.email, employee.phone, employee.designation)
                .any { it.contains(query, ignoreCase = true) }
        }
        _uiState.value = state.copy(
            visibleEmployees = visible,
            content = if (visible.isEmpty()) EmployeeManagementUiState.Content.Empty else EmployeeManagementUiState.Content.Loaded,
        )
    }

    private fun requestStatusChange(employee: EmployeeListItemUi) {
        if (!isActiveAdmin() || _uiState.value.updatingEmployeeUid != null) {
            if (!isActiveAdmin()) unauthorized()
            return
        }
        _uiState.value = _uiState.value.copy(pendingStatusChange = employee)
    }

    private fun changeStatus() {
        val employee = _uiState.value.pendingStatusChange ?: return
        if (_uiState.value.updatingEmployeeUid != null) return
        if (!isActiveAdmin()) {
            unauthorized()
            return
        }
        _uiState.value = _uiState.value.copy(updatingEmployeeUid = employee.uid, pendingStatusChange = null)
        viewModelScope.launch {
            when (val result = setEmployeeActive(employee.uid, !employee.active)) {
                is EmployeeResult.Success -> Unit
                is EmployeeResult.Error -> {
                    _uiState.value = _uiState.value.copy(updatingEmployeeUid = null)
                    sendEffect(EmployeeManagementUiEffect.ShowMessage(result.error.messageRes()))
                }
            }
        }
    }

    private fun changeRole() {
        val employee = _uiState.value.selectedEmployee ?: return
        val role = _uiState.value.selectedRole ?: return
        if (employee.role == role || _uiState.value.updatingEmployeeUid != null) return
        if (!isActiveAdmin()) {
            unauthorized()
            return
        }
        _uiState.value = _uiState.value.copy(updatingEmployeeUid = employee.uid)
        viewModelScope.launch {
            when (val result = updateEmployeeRole(employee.uid, role)) {
                is EmployeeResult.Success -> _uiState.value = _uiState.value.copy(
                    updatingEmployeeUid = null,
                    selectedEmployee = null,
                    selectedRole = null,
                )
                is EmployeeResult.Error -> {
                    _uiState.value = _uiState.value.copy(updatingEmployeeUid = null)
                    sendEffect(EmployeeManagementUiEffect.ShowMessage(result.error.messageRes()))
                }
            }
        }
    }

    private fun Employee.toListItemUi(): EmployeeListItemUi = EmployeeListItemUi(
        uid = uid,
        name = name,
        email = email,
        phone = phone,
        role = role,
        active = active,
        firstLogin = firstLogin,
        designation = designation,
        createdDate = createdAtMillis?.let { DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(it)) },
    )

    private fun EmployeeError.messageRes(): Int = when (this) {
        EmployeeError.UNAUTHORIZED -> R.string.employee_management_unauthorized
        EmployeeError.EMPLOYEE_NOT_FOUND -> R.string.employee_management_not_found
        EmployeeError.NETWORK_UNAVAILABLE -> R.string.employee_management_network_error
        EmployeeError.UNKNOWN -> R.string.employee_management_error
        EmployeeError.DUPLICATE_EMAIL -> R.string.employee_form_duplicate_email
        EmployeeError.DUPLICATE_PHONE -> R.string.employee_form_duplicate_phone
        EmployeeError.VALIDATION_FAILED -> R.string.employee_form_validation_error
    }

    private fun isActiveAdmin(): Boolean =
        ((sessionManager.currentUser.value as? CurrentUserState.Authenticated)?.user)?.let {
            it.active && it.role == UserRole.ADMIN
        } == true

    private fun unauthorized() {
        _uiState.value = _uiState.value.copy(content = EmployeeManagementUiState.Content.Error)
        sendEffect(EmployeeManagementUiEffect.UnauthorizedAccess(R.string.employee_management_unauthorized))
    }

    private fun sendEffect(effect: EmployeeManagementUiEffect) {
        viewModelScope.launch { effectsChannel.send(effect) }
    }
}
