package com.brandcrafts.erp.domain.usecase.employee

import com.brandcrafts.erp.domain.model.EmployeeUpdateCommand
import com.brandcrafts.erp.domain.repository.EmployeeRepository
import javax.inject.Inject

class UpdateEmployeeUseCase @Inject constructor(private val repository: EmployeeRepository) {
    suspend operator fun invoke(command: EmployeeUpdateCommand) = repository.updateEmployee(command)
}
