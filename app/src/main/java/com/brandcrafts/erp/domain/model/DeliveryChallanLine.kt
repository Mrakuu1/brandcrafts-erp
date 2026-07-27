package com.brandcrafts.erp.domain.model

import java.math.BigDecimal

data class DeliveryChallanLine(
    val id: String,
    val materialId: String,
    val description: String,
    val quantity: BigDecimal,
    val unit: String,
    val sortOrder: Int,
)
