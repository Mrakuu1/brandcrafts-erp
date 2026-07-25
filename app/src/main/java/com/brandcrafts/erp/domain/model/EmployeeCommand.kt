package com.brandcrafts.erp.domain.model

data class EmployeeCreateCommand(
    val name: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val active: Boolean,
    val temporaryPassword: String,
)

data class EmployeeUpdateCommand(
    val uid: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: UserRole,
    val active: Boolean,
)
