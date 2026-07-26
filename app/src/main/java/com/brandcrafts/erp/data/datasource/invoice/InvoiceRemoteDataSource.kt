package com.brandcrafts.erp.data.datasource.invoice

import com.brandcrafts.erp.data.model.invoice.InvoiceDto
import com.brandcrafts.erp.data.model.invoice.InvoiceLineDto
import com.brandcrafts.erp.domain.model.InvoiceSummary
import kotlinx.coroutines.flow.Flow

interface InvoiceRemoteDataSource {
    fun observeInvoiceParents(): Flow<List<InvoiceSummary>>
    suspend fun getInvoiceParent(invoiceId: String): InvoiceDto?
    suspend fun getInvoiceLines(invoiceId: String): List<InvoiceLineDto>
}

/** Future transactional write boundary; deliberately separate from the implemented read source. */
interface InvoiceWriteRemoteDataSource {
    suspend fun createInvoice(request: com.brandcrafts.erp.domain.model.InvoiceCreateRequest): String
    suspend fun updateDraftInvoice(request: com.brandcrafts.erp.domain.model.InvoiceDraftUpdateRequest)
    suspend fun issueInvoice(invoiceId: String)
    suspend fun cancelInvoice(invoiceId: String)
    suspend fun recordInvoicePayment(request: com.brandcrafts.erp.domain.model.InvoicePaymentRequest)
}
