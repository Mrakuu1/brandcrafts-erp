package com.brandcrafts.erp.domain.usecase.quotation
import com.brandcrafts.erp.domain.model.QuotationDraft
import com.brandcrafts.erp.domain.repository.QuotationRepository
import javax.inject.Inject
class CreateQuotationUseCase @Inject constructor(private val repository:QuotationRepository){suspend operator fun invoke(draft:QuotationDraft)=repository.createQuotation(draft)}
