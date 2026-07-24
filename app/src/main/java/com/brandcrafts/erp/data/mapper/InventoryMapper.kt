package com.brandcrafts.erp.data.mapper

import com.brandcrafts.erp.data.model.FirestoreInventoryItem
import com.brandcrafts.erp.domain.model.InventoryItem
import com.brandcrafts.erp.domain.model.InventoryItemInput
import com.brandcrafts.erp.domain.model.InventoryItemUpdate
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue

fun DocumentSnapshot.toFirestoreInventoryItem(): FirestoreInventoryItem = FirestoreInventoryItem(
    id = getString("id") ?: id,
    name = getString("name") ?: "",
    sku = getString("sku") ?: "",
    category = getString("category") ?: "",
    unit = getString("unit") ?: "",
    availableQuantity = getDouble("availableQuantity") ?: 0.0,
    minimumQuantity = getDouble("minimumQuantity") ?: 0.0,
    purchasePrice = getDouble("purchasePrice") ?: 0.0,
    sellingPrice = getDouble("sellingPrice") ?: 0.0,
    description = getString("description") ?: "",
    active = getBoolean("active") ?: false,
    createdAt = getTimestamp("createdAt"),
    updatedAt = getTimestamp("updatedAt"),
    createdBy = getString("createdBy") ?: "",
    updatedBy = getString("updatedBy") ?: "",
)

fun FirestoreInventoryItem.toDomain(): InventoryItem = InventoryItem(
    id = id,
    name = name,
    sku = sku,
    category = category,
    unit = unit,
    availableQuantity = availableQuantity,
    minimumQuantity = minimumQuantity,
    purchasePrice = purchasePrice,
    sellingPrice = sellingPrice,
    description = description,
    active = active,
    createdAtMillis = createdAt?.toDate()?.time,
    updatedAtMillis = updatedAt?.toDate()?.time,
    createdBy = createdBy,
    updatedBy = updatedBy,
)

fun InventoryItemInput.toFirestoreInventoryItem(createdBy: String): FirestoreInventoryItem = FirestoreInventoryItem(
    id = "",
    name = name,
    sku = sku,
    category = category,
    unit = unit,
    availableQuantity = availableQuantity,
    minimumQuantity = minimumQuantity,
    purchasePrice = purchasePrice,
    sellingPrice = sellingPrice,
    description = description,
    active = active,
    createdAt = null,
    updatedAt = null,
    createdBy = createdBy,
    updatedBy = createdBy,
)

fun InventoryItemUpdate.toFirestoreInventoryItem(updatedBy: String): FirestoreInventoryItem = FirestoreInventoryItem(
    id = id,
    name = name,
    sku = sku,
    category = category,
    unit = unit,
    availableQuantity = availableQuantity,
    minimumQuantity = minimumQuantity,
    purchasePrice = purchasePrice,
    sellingPrice = sellingPrice,
    description = description,
    active = active,
    createdAt = null,
    updatedAt = null,
    createdBy = "",
    updatedBy = updatedBy,
)

fun FirestoreInventoryItem.toCreateMap(id: String): Map<String, Any?> = mapOf(
    "id" to id,
    "name" to name,
    "sku" to sku,
    "category" to category,
    "unit" to unit,
    "availableQuantity" to availableQuantity,
    "minimumQuantity" to minimumQuantity,
    "purchasePrice" to purchasePrice,
    "sellingPrice" to sellingPrice,
    "description" to description,
    "active" to active,
    "createdAt" to FieldValue.serverTimestamp(),
    "updatedAt" to FieldValue.serverTimestamp(),
    "createdBy" to createdBy,
    "updatedBy" to updatedBy,
)

fun FirestoreInventoryItem.toUpdateMap(): Map<String, Any?> = mapOf(
    "name" to name,
    "sku" to sku,
    "category" to category,
    "unit" to unit,
    "availableQuantity" to availableQuantity,
    "minimumQuantity" to minimumQuantity,
    "purchasePrice" to purchasePrice,
    "sellingPrice" to sellingPrice,
    "description" to description,
    "active" to active,
    "updatedAt" to FieldValue.serverTimestamp(),
    "updatedBy" to updatedBy,
)
