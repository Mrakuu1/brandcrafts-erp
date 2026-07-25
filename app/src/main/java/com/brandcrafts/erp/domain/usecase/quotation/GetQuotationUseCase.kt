package com.brandcrafts.erp.domain.usecase.quotation
import com.brandcrafts.erp.domain.repository.QuotationRepository
import javax.inject.Inject
class GetQuotationUseCase @Inject constructor(private val repository:QuotationRepository){suspend operator fun invoke(id:String)=repository.getQuotation(id)}
