package com.brandcrafts.erp.feature.employee

import com.brandcrafts.erp.domain.model.UserRole

data class EmployeeManagementUiState(
    val content: Content = Content.Loading,
    val searchQuery: String = "",
    val allEmployees: List<EmployeeListItemUi> = emptyList(),
    val visibleEmployees: List<EmployeeListItemUi> = emptyList(),
    val updatingEmployeeUid: String? = null,
    val selectedEmployee: EmployeeListItemUi? = null,
    val selectedRole: UserRole? = null,
    val pendingStatusChange: EmployeeListItemUi? = null,
) {
    sealed interface Content {
        data object Loading : Content
        data object Loaded : Content
        data object Empty : Content
        data object Error : Content
    }
}

data class EmployeeListItemUi(
    val uid: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val active: Boolean,
    val firstLogin: Boolean,
    val designation: String,
    val createdDate: String?,
)
