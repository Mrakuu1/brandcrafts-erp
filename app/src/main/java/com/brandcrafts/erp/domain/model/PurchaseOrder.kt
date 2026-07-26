package com.brandcrafts.erp.domain.model

import java.math.BigDecimal

data class PurchaseOrder(
    val id: String,
    val number: String,
    val supplierId: String,
    val dateMillis: Long?,
    val expectedDeliveryDateMillis: Long?,
    val supplierReferenceNumber: String,
    val remarks: String,
    val status: PurchaseOrderStatus,
    val total: BigDecimal,
    val createdAtMillis: Long?,
    val updatedAtMillis: Long?,
    val createdBy: String,
    val updatedBy: String,
    val approvedAtMillis: Long?,
    val approvedBy: String,
    val cancelledAtMillis: Long?,
    val cancelledBy: String,
    val lines: List<PurchaseOrderLine> = emptyList(),
)

data class PurchaseOrderLine(
    val id: String,
    val materialId: String,
    val description: String,
    val quantity: BigDecimal,
    val unit: String,
    val unitPrice: BigDecimal,
    val lineTotal: BigDecimal,
    val sortOrder: Int,
)

data class PurchaseOrderDraft(
    val supplierId: String,
    val dateMillis: Long,
    val expectedDeliveryDateMillis: Long?,
    val supplierReferenceNumber: String,
    val remarks: String,
    val lines: List<PurchaseOrderDraftLine>,
)

data class PurchaseOrderDraftLine(
    val id: String? = null,
    val materialId: String,
    val description: String,
    val quantity: BigDecimal,
    val unit: String,
    val unitPrice: BigDecimal,
)

enum class PurchaseOrderStatus { DRAFT, APPROVED, CANCELLED }
