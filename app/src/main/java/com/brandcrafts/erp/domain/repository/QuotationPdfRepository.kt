package com.brandcrafts.erp.domain.repository

import com.brandcrafts.erp.domain.model.QuotationPdf

interface QuotationPdfRepository {
    suspend fun generate(quotationId: String): Result<QuotationPdf>
}
