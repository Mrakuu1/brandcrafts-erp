package com.brandcrafts.erp.data.datasource.employee

import com.brandcrafts.erp.domain.model.EmployeeCreateCommand
import com.brandcrafts.erp.domain.model.EmployeeUpdateCommand

interface EmployeeFunctionsDataSource {
    suspend fun createEmployee(command: EmployeeCreateCommand): EmployeeFunctionEmployee
    suspend fun updateEmployee(command: EmployeeUpdateCommand): EmployeeFunctionEmployee
}

data class EmployeeFunctionEmployee(
    val uid: String,
    val name: String,
    val email: String,
    val phone: String,
    val role: String,
    val active: Boolean,
    val firstLogin: Boolean,
)
