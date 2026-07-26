package com.brandcrafts.erp.domain.usecase.invoice

import com.brandcrafts.erp.domain.repository.InvoiceRepository
import javax.inject.Inject

class ObserveInvoicesUseCase @Inject constructor(
    private val repository: InvoiceRepository,
) {
    operator fun invoke() = repository.observeInvoices()
}
