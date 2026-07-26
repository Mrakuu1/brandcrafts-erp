package com.brandcrafts.erp.domain.repository

import com.brandcrafts.erp.domain.model.Invoice
import com.brandcrafts.erp.domain.model.InvoiceSummary
import com.brandcrafts.erp.domain.model.InvoiceCreateRequest
import com.brandcrafts.erp.domain.model.InvoiceDraftUpdateRequest
import com.brandcrafts.erp.domain.model.InvoicePaymentRequest
import kotlinx.coroutines.flow.Flow

interface InvoiceRepository {
    fun observeInvoices(): Flow<Result<List<InvoiceSummary>>>
    suspend fun getInvoice(id: String): Result<Invoice>
    suspend fun getInvoiceLines(invoiceId: String): Result<List<com.brandcrafts.erp.domain.model.InvoiceLine>>
    suspend fun createInvoice(request: InvoiceCreateRequest): Result<String>
    suspend fun updateDraftInvoice(request: InvoiceDraftUpdateRequest): Result<Unit>
    suspend fun issueInvoice(invoiceId: String): Result<Unit>
    suspend fun cancelInvoice(invoiceId: String): Result<Unit>
    suspend fun recordPayment(request: InvoicePaymentRequest): Result<Unit>
}
