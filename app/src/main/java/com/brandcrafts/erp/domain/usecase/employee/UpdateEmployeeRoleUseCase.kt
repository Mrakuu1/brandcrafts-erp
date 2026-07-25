package com.brandcrafts.erp.domain.usecase.employee

import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.domain.repository.EmployeeRepository
import javax.inject.Inject

class UpdateEmployeeRoleUseCase @Inject constructor(
    private val repository: EmployeeRepository,
) {
    suspend operator fun invoke(uid: String, role: UserRole) = repository.updateEmployeeRole(uid, role)
}
