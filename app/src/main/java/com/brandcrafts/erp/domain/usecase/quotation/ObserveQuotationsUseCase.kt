package com.brandcrafts.erp.domain.usecase.quotation
import com.brandcrafts.erp.domain.repository.QuotationRepository
import javax.inject.Inject
class ObserveQuotationsUseCase @Inject constructor(private val repository:QuotationRepository){operator fun invoke()=repository.observeQuotations()}
