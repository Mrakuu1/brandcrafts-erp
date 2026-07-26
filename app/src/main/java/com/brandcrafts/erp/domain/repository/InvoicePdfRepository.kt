package com.brandcrafts.erp.domain.repository

import com.brandcrafts.erp.domain.model.InvoicePdf

interface InvoicePdfRepository {
    suspend fun generate(invoiceId: String): Result<InvoicePdf>
}
