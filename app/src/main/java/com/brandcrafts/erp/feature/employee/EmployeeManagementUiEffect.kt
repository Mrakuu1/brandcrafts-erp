package com.brandcrafts.erp.feature.employee

import androidx.annotation.StringRes

sealed interface EmployeeManagementUiEffect {
    data class ShowMessage(@param:StringRes val messageRes: Int) : EmployeeManagementUiEffect
    data class UnauthorizedAccess(@param:StringRes val messageRes: Int) : EmployeeManagementUiEffect
    data object OpenEmployeeCreate : EmployeeManagementUiEffect
    data class OpenEmployeeEdit(val uid: String) : EmployeeManagementUiEffect
}
