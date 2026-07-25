package com.brandcrafts.erp.feature.employee

import androidx.annotation.StringRes
import com.brandcrafts.erp.domain.model.UserRole

enum class EmployeeFormMode { CREATE, EDIT }

data class EmployeeFormUiState(
    val mode: EmployeeFormMode = EmployeeFormMode.CREATE,
    val uid: String? = null,
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val role: UserRole = UserRole.EMPLOYEE,
    val active: Boolean = true,
    val temporaryPassword: String = "",
    val isInitialLoading: Boolean = true,
    val isSaving: Boolean = false,
    val loadError: EmployeeFormError? = null,
    val saveError: EmployeeFormError? = null,
    val errors: EmployeeFormFieldErrors = EmployeeFormFieldErrors(),
)

data class EmployeeFormFieldErrors(
    @param:StringRes val name: Int? = null,
    @param:StringRes val email: Int? = null,
    @param:StringRes val phone: Int? = null,
    @param:StringRes val temporaryPassword: Int? = null,
)

enum class EmployeeFormError { UNAUTHORIZED, NOT_FOUND, DUPLICATE_EMAIL, DUPLICATE_PHONE, NETWORK, VALIDATION, UNKNOWN }
