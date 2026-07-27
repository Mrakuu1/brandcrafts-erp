package com.brandcrafts.erp.data.model.deliverychallan

data class DeliveryChallanDto(
    val id: String? = null,
    val dcNumber: String? = null,
    val customerId: String? = null,
    val deliveryAddress: String? = null,
    val date: Any? = null,
    val sourceType: String? = null,
    val sourceInvoiceId: String? = null,
    val sourceInvoiceNumber: String? = null,
    val vehicleNumber: String? = null,
    val driverName: String? = null,
    val notes: String? = null,
    val status: String? = null,
    val createdAt: Any? = null,
    val updatedAt: Any? = null,
    val createdBy: String? = null,
    val updatedBy: String? = null,
    val dispatchedAt: Any? = null,
    val dispatchedBy: String? = null,
    val cancelledAt: Any? = null,
    val cancelledBy: String? = null,
)

data class DeliveryChallanLineDto(
    val itemId: String? = null,
    val materialId: String? = null,
    val description: String? = null,
    val quantity: String? = null,
    val unit: String? = null,
    val sortOrder: Int? = null,
)
