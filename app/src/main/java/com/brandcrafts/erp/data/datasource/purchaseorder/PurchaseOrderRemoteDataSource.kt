package com.brandcrafts.erp.data.datasource.purchaseorder

import com.brandcrafts.erp.domain.model.PurchaseOrder
import kotlinx.coroutines.flow.Flow

interface PurchaseOrderRemoteDataSource {
    fun observePurchaseOrders(): Flow<List<PurchaseOrder>>
}
