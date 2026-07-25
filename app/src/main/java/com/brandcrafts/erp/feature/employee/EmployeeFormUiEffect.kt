package com.brandcrafts.erp.feature.employee

sealed interface EmployeeFormUiEffect {
    data object NavigateBack : EmployeeFormUiEffect
    data class EmployeeSaved(val mode: EmployeeFormMode) : EmployeeFormUiEffect
}
