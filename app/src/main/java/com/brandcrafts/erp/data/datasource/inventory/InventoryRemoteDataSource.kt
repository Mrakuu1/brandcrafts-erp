package com.brandcrafts.erp.data.datasource.inventory

import com.brandcrafts.erp.data.model.FirestoreInventoryItem
import kotlinx.coroutines.flow.Flow

interface InventoryRemoteDataSource {
    fun observeItems(): Flow<List<FirestoreInventoryItem>>

    suspend fun getItem(id: String): FirestoreInventoryItem?

    suspend fun createItem(item: FirestoreInventoryItem)

    suspend fun updateItem(item: FirestoreInventoryItem)

    suspend fun isSkuInUse(sku: String, excludingId: String? = null): Boolean
}
