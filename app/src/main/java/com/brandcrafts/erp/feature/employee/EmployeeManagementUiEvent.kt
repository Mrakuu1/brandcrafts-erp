package com.brandcrafts.erp.feature.employee

import com.brandcrafts.erp.domain.model.UserRole

sealed interface EmployeeManagementUiEvent {
    data class SearchChanged(val value: String) : EmployeeManagementUiEvent
    data class StatusChangeRequested(val employee: EmployeeListItemUi) : EmployeeManagementUiEvent
    data object StatusChangeConfirmed : EmployeeManagementUiEvent
    data object StatusChangeDismissed : EmployeeManagementUiEvent
    data object AddEmployeeClicked : EmployeeManagementUiEvent
    data class EditEmployeeRequested(val employee: EmployeeListItemUi) : EmployeeManagementUiEvent
    data class EmployeeClicked(val employee: EmployeeListItemUi) : EmployeeManagementUiEvent
    data class RoleSelected(val role: UserRole) : EmployeeManagementUiEvent
    data object RoleChangeConfirmed : EmployeeManagementUiEvent
    data object EditEmployeeClicked : EmployeeManagementUiEvent
    data object EmployeeActionsDismissed : EmployeeManagementUiEvent
    data object RetryClicked : EmployeeManagementUiEvent
}
