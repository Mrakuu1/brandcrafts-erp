package com.brandcrafts.erp.data.model

data class FirestoreStockIn(
    val materialId: String,
    val quantity: Double,
    val unit: String,
    val referenceId: String,
    val remarks: String,
    val performedBy: String,
    val performedByName: String,
)
