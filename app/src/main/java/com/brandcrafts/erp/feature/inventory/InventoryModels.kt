package com.brandcrafts.erp.feature.inventory

import com.brandcrafts.erp.domain.model.InventoryItem

data class InventoryListItem(
    val id: String,
    val name: String,
    val sku: String,
    val category: String,
    val unit: String,
    val availableQuantity: Double,
    val minimumQuantity: Double,
    val purchasePrice: Double,
    val sellingPrice: Double,
    val active: Boolean,
) {
    val isLowStock: Boolean
        get() = availableQuantity <= minimumQuantity
}

fun InventoryItem.toInventoryListItem(): InventoryListItem = InventoryListItem(
    id = id,
    name = name,
    sku = sku,
    category = category,
    unit = unit,
    availableQuantity = availableQuantity,
    minimumQuantity = minimumQuantity,
    purchasePrice = purchasePrice,
    sellingPrice = sellingPrice,
    active = active,
)
