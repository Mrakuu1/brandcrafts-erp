package com.brandcrafts.erp.domain.repository

import com.brandcrafts.erp.core.result.EmployeeResult
import com.brandcrafts.erp.domain.model.Employee
import com.brandcrafts.erp.domain.model.EmployeeCreateCommand
import com.brandcrafts.erp.domain.model.EmployeeUpdateCommand
import com.brandcrafts.erp.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface EmployeeRepository {
    fun observeEmployees(): Flow<EmployeeResult<List<Employee>>>
    suspend fun setEmployeeActive(uid: String, active: Boolean): EmployeeResult<Unit>
    suspend fun updateEmployeeRole(uid: String, role: UserRole): EmployeeResult<Unit>
    suspend fun getEmployee(uid: String): EmployeeResult<Employee>
    suspend fun createEmployee(command: EmployeeCreateCommand): EmployeeResult<Employee>
    suspend fun updateEmployee(command: EmployeeUpdateCommand): EmployeeResult<Employee>
}
