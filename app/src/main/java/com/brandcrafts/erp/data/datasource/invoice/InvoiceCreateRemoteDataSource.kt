package com.brandcrafts.erp.data.datasource.invoice

import com.brandcrafts.erp.domain.model.InvoiceCreateRequest

interface InvoiceCreateRemoteDataSource {
    suspend fun createInvoice(request: InvoiceCreateRequest): String
}
