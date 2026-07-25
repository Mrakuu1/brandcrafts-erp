package com.brandcrafts.erp.domain.usecase.company
import com.brandcrafts.erp.domain.repository.CompanyConfigRepository
import javax.inject.Inject
class GetCompanyConfigUseCase @Inject constructor(private val repository:CompanyConfigRepository){suspend operator fun invoke()=repository.getCompanyConfig()}
