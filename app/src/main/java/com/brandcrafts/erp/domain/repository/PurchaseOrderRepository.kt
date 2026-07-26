package com.brandcrafts.erp.domain.repository

import com.brandcrafts.erp.domain.model.PurchaseOrder
import com.brandcrafts.erp.domain.model.PurchaseOrderDraft
import kotlinx.coroutines.flow.Flow

interface PurchaseOrderRepository {
    fun observePurchaseOrders(): Flow<Result<List<PurchaseOrder>>>
    suspend fun getPurchaseOrder(id: String): Result<PurchaseOrder>
    suspend fun createPurchaseOrder(draft: PurchaseOrderDraft): Result<String>
    suspend fun updatePurchaseOrder(id: String, draft: PurchaseOrderDraft): Result<Unit>
    suspend fun approvePurchaseOrder(id: String): Result<Unit>
    suspend fun cancelPurchaseOrder(id: String): Result<Unit>
}
