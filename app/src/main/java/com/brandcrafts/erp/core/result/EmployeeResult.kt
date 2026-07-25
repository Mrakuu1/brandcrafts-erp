package com.brandcrafts.erp.core.result

sealed interface EmployeeResult<out T> {
    data class Success<T>(val data: T) : EmployeeResult<T>
    data class Error(val error: EmployeeError) : EmployeeResult<Nothing>
}

enum class EmployeeError {
    UNAUTHORIZED,
    EMPLOYEE_NOT_FOUND,
    DUPLICATE_EMAIL,
    DUPLICATE_PHONE,
    VALIDATION_FAILED,
    NETWORK_UNAVAILABLE,
    UNKNOWN,
}
