package com.brandcrafts.erp.domain.repository
import com.brandcrafts.erp.domain.model.InventoryTransaction
import kotlinx.coroutines.flow.Flow
interface InventoryTransactionRepository{fun observeRecent(materialId:String):Flow<Result<List<InventoryTransaction>>>}
