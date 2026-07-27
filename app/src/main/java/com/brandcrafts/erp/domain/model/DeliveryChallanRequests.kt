package com.brandcrafts.erp.domain.model

data class DeliveryChallanCreateRequest(
    val customerId: String,
    val deliveryAddress: String,
    val dateMillis: Long,
    val sourceType: DeliveryChallanSourceType,
    val sourceInvoiceId: String? = null,
    val sourceInvoiceNumber: String? = null,
    val vehicleNumber: String,
    val driverName: String,
    val notes: String,
    val lines: List<DeliveryChallanLine>,
)

data class DeliveryChallanDraftUpdateRequest(
    val challanId: String,
    val customerId: String,
    val deliveryAddress: String,
    val dateMillis: Long,
    val sourceType: DeliveryChallanSourceType,
    val sourceInvoiceId: String? = null,
    val sourceInvoiceNumber: String? = null,
    val vehicleNumber: String,
    val driverName: String,
    val notes: String,
    val lines: List<DeliveryChallanLine>,
)

data class DeliveryChallanDispatchRequest(val challanId: String)

data class DeliveryChallanInvoiceCreateRequest(
    val invoiceId: String,
    val deliveryAddress: String,
    val dateMillis: Long,
    val vehicleNumber: String,
    val driverName: String,
    val notes: String,
    val lines: List<DeliveryChallanInvoiceLineRequest>,
)

data class DeliveryChallanInvoiceLineRequest(
    val sourceInvoiceLineId: String,
    val quantity: java.math.BigDecimal,
)
