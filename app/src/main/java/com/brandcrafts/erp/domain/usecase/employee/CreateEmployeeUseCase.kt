package com.brandcrafts.erp.domain.usecase.employee

import com.brandcrafts.erp.domain.model.EmployeeCreateCommand
import com.brandcrafts.erp.domain.repository.EmployeeRepository
import javax.inject.Inject

class CreateEmployeeUseCase @Inject constructor(private val repository: EmployeeRepository) {
    suspend operator fun invoke(command: EmployeeCreateCommand) = repository.createEmployee(command)
}
