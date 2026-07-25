package com.brandcrafts.erp.data.datasource.employee

import com.brandcrafts.erp.data.model.employee.EmployeeDto
import com.brandcrafts.erp.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface EmployeeRemoteDataSource {
    fun observeEmployees(): Flow<List<EmployeeDto>>
    suspend fun getEmployee(uid: String): EmployeeDto?
    suspend fun setEmployeeActive(uid: String, active: Boolean, updatedBy: String, updatedByName: String)
    suspend fun updateEmployeeRole(uid: String, role: UserRole, updatedBy: String, updatedByName: String)
}
