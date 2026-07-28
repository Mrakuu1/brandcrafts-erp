package com.brandcrafts.erp.domain.usecase.quotation

import com.brandcrafts.erp.domain.repository.QuotationPdfRepository
import javax.inject.Inject

class GenerateQuotationPdfUseCase @Inject constructor(
    private val repository: QuotationPdfRepository,
) {
    suspend operator fun invoke(quotationId: String) = repository.generate(quotationId)
}
