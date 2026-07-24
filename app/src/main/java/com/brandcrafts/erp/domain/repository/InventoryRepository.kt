package com.brandcrafts.erp.domain.repository

import com.brandcrafts.erp.core.result.InventoryResult
import com.brandcrafts.erp.domain.model.InventoryItem
import com.brandcrafts.erp.domain.model.InventoryItemInput
import com.brandcrafts.erp.domain.model.InventoryItemUpdate
import kotlinx.coroutines.flow.Flow

interface InventoryRepository {
    fun observeInventoryItems(): Flow<InventoryResult<List<InventoryItem>>>
    suspend fun getInventoryItem(id: String): InventoryResult<InventoryItem>
    suspend fun createInventoryItem(input: InventoryItemInput): InventoryResult<Unit>
    suspend fun updateInventoryItem(input: InventoryItemUpdate): InventoryResult<Unit>
    fun searchInventoryItems(query: String): Flow<InventoryResult<List<InventoryItem>>>
}
