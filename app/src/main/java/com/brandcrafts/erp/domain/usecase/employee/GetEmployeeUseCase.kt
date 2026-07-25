package com.brandcrafts.erp.domain.usecase.employee

import com.brandcrafts.erp.domain.repository.EmployeeRepository
import javax.inject.Inject

class GetEmployeeUseCase @Inject constructor(private val repository: EmployeeRepository) {
    suspend operator fun invoke(uid: String) = repository.getEmployee(uid)
}
