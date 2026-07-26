package com.brandcrafts.erp.domain.usecase.invoice

import com.brandcrafts.erp.domain.model.InvoiceCreateRequest
import com.brandcrafts.erp.domain.repository.InvoiceRepository
import javax.inject.Inject

class CreateInvoiceUseCase @Inject constructor(
    private val repository: InvoiceRepository,
) {
    suspend operator fun invoke(request: InvoiceCreateRequest) = repository.createInvoice(request)
}
