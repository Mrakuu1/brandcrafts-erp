package com.brandcrafts.erp.data.model.invoice

import com.google.firebase.Timestamp

data class InvoiceDto(
    val id: String? = null, val invoiceNumber: String? = null, val customerId: String? = null,
    val invoiceDate: Any? = null, val dueDate: Any? = null, val status: String? = null,
    val subtotal: String? = null, val discountTotal: String? = null, val taxTotal: String? = null,
    val grandTotal: String? = null, val paidAmount: String? = null, val paymentStatus: String? = null,
    val remarks: String? = null, val createdAt: Timestamp? = null, val createdBy: String? = null,
    val updatedAt: Timestamp? = null, val updatedBy: String? = null, val issuedAt: Timestamp? = null,
    val issuedBy: String? = null, val cancelledAt: Timestamp? = null, val cancelledBy: String? = null,
)

data class InvoiceLineDto(
    val itemId: String? = null, val materialId: String? = null, val description: String? = null,
    val quantity: String? = null, val unit: String? = null, val unitPrice: String? = null,
    val discountPercent: String? = null, val taxPercent: String? = null, val lineSubtotal: String? = null,
    val lineDiscount: String? = null, val taxableAmount: String? = null, val lineTax: String? = null,
    val lineTotal: String? = null, val sortOrder: Int? = null,
)
