package com.brandcrafts.erp.feature.employee

import com.brandcrafts.erp.domain.model.UserRole

sealed interface EmployeeFormUiEvent {
    data class NameChanged(val value: String) : EmployeeFormUiEvent
    data class EmailChanged(val value: String) : EmployeeFormUiEvent
    data class PhoneChanged(val value: String) : EmployeeFormUiEvent
    data class RoleChanged(val value: UserRole) : EmployeeFormUiEvent
    data class ActiveChanged(val value: Boolean) : EmployeeFormUiEvent
    data class TemporaryPasswordChanged(val value: String) : EmployeeFormUiEvent
    data object SaveClicked : EmployeeFormUiEvent
    data object CancelClicked : EmployeeFormUiEvent
    data object RetryLoadClicked : EmployeeFormUiEvent
}
