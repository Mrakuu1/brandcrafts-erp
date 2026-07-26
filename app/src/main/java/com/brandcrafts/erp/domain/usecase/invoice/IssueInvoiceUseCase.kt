package com.brandcrafts.erp.domain.usecase.invoice

import com.brandcrafts.erp.domain.repository.InvoiceRepository
import javax.inject.Inject

class IssueInvoiceUseCase @Inject constructor(
    private val repository: InvoiceRepository,
) {
    suspend operator fun invoke(invoiceId: String) = repository.issueInvoice(invoiceId)
}
