package com.brandcrafts.erp.domain.usecase.invoice

import com.brandcrafts.erp.domain.model.InvoicePaymentRequest
import com.brandcrafts.erp.domain.repository.InvoiceRepository
import javax.inject.Inject

class RecordInvoicePaymentUseCase @Inject constructor(
    private val repository: InvoiceRepository,
) {
    suspend operator fun invoke(request: InvoicePaymentRequest) =
        repository.recordPayment(request)
}
