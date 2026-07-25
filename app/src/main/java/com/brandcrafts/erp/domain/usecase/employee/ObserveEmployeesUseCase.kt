package com.brandcrafts.erp.domain.usecase.employee

import com.brandcrafts.erp.domain.repository.EmployeeRepository
import javax.inject.Inject

class ObserveEmployeesUseCase @Inject constructor(
    private val repository: EmployeeRepository,
) {
    operator fun invoke() = repository.observeEmployees()
}
