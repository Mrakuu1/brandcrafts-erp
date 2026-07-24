package com.brandcrafts.erp.domain.model

data class InventoryItem(
    val id: String,
    val name: String,
    val sku: String,
    val category: String,
    val unit: String,
    val availableQuantity: Double,
    val minimumQuantity: Double,
    val purchasePrice: Double,
    val sellingPrice: Double,
    val description: String,
    val active: Boolean,
    val createdAtMillis: Long?,
    val updatedAtMillis: Long?,
    val createdBy: String,
    val updatedBy: String,
)

data class InventoryItemInput(
    val name: String,
    val sku: String,
    val category: String,
    val unit: String,
    val availableQuantity: Double,
    val minimumQuantity: Double,
    val purchasePrice: Double,
    val sellingPrice: Double,
    val description: String,
    val active: Boolean,
)

data class InventoryItemUpdate(
    val id: String,
    val name: String,
    val sku: String,
    val category: String,
    val unit: String,
    val availableQuantity: Double,
    val minimumQuantity: Double,
    val purchasePrice: Double,
    val sellingPrice: Double,
    val description: String,
    val active: Boolean,
)
