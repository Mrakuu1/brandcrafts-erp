package com.brandcrafts.erp.domain.model

data class DeliveryChallanSummary(
    val id: String,
    val number: String,
    val customerId: String,
    val dateMillis: Long,
    val sourceType: DeliveryChallanSourceType,
    val sourceInvoiceNumber: String?,
    val status: DeliveryChallanStatus,
)
