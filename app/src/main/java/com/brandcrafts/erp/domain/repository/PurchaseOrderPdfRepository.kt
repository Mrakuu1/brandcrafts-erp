package com.brandcrafts.erp.domain.repository

import com.brandcrafts.erp.domain.model.PurchaseOrderPdf

interface PurchaseOrderPdfRepository {
    suspend fun generate(purchaseOrderId: String): Result<PurchaseOrderPdf>
}
