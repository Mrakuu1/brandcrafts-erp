package com.brandcrafts.erp.domain.repository

import com.brandcrafts.erp.domain.model.DeliveryChallanPdf

interface DeliveryChallanPdfRepository {
    suspend fun generate(challanId: String): Result<DeliveryChallanPdf>
}
