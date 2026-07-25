package com.brandcrafts.erp.domain.usecase.quotation
import com.brandcrafts.erp.domain.model.QuotationDraft
import com.brandcrafts.erp.domain.repository.QuotationRepository
import javax.inject.Inject
class UpdateQuotationUseCase @Inject constructor(private val repository:QuotationRepository){suspend operator fun invoke(id:String,draft:QuotationDraft)=repository.updateQuotation(id,draft)}
