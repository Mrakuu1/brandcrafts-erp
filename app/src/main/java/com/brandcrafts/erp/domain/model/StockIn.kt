package com.brandcrafts.erp.domain.model

data class StockInInput(
    val materialId: String,
    val quantity: Double,
    val referenceId: String,
    val remarks: String,
)
