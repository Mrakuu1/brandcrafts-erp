package com.brandcrafts.erp.domain.usecase.invoice

import com.brandcrafts.erp.domain.repository.InvoicePdfRepository
import javax.inject.Inject

class GenerateInvoicePdfUseCase @Inject constructor(
    private val repository: InvoicePdfRepository,
) {
    suspend operator fun invoke(invoiceId: String) = repository.generate(invoiceId)
}
