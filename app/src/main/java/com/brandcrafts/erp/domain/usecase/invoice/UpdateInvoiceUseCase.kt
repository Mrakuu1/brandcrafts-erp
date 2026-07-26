package com.brandcrafts.erp.domain.usecase.invoice

import com.brandcrafts.erp.domain.model.InvoiceDraftUpdateRequest
import com.brandcrafts.erp.domain.repository.InvoiceRepository
import javax.inject.Inject

class UpdateInvoiceUseCase @Inject constructor(
    private val repository: InvoiceRepository,
) {
    suspend operator fun invoke(request: InvoiceDraftUpdateRequest) =
        repository.updateDraftInvoice(request)
}
