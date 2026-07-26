package com.brandcrafts.erp.domain.usecase.purchaseorder

import com.brandcrafts.erp.domain.repository.PurchaseOrderPdfRepository
import javax.inject.Inject

class GeneratePurchaseOrderPdfUseCase @Inject constructor(
    private val repository: PurchaseOrderPdfRepository,
) {
    suspend operator fun invoke(purchaseOrderId: String) = repository.generate(purchaseOrderId)
}
