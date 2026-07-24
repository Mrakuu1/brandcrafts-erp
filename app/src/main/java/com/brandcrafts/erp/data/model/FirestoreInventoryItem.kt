package com.brandcrafts.erp.data.model

import com.google.firebase.Timestamp

data class FirestoreInventoryItem(
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
    val createdAt: Timestamp?,
    val updatedAt: Timestamp?,
    val createdBy: String,
    val updatedBy: String,
)
