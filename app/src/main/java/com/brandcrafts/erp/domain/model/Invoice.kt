package com.brandcrafts.erp.domain.model

import java.math.BigDecimal

data class InvoiceSummary(
    val id: String,
    val number: String,
    val customerId: String,
    val customerName: String?,
    val invoiceDateMillis: Long,
    val dueDateMillis: Long?,
    val status: InvoiceStatus,
    val paymentStatus: InvoicePaymentStatus,
    val grandTotal: BigDecimal,
    val paidAmount: BigDecimal,
) {
    val outstandingAmount: BigDecimal get() = grandTotal.subtract(paidAmount)
    fun isOverdue(currentDateMillis: Long): Boolean = status == InvoiceStatus.ISSUED && dueDateMillis != null && currentDateMillis > dueDateMillis && outstandingAmount > BigDecimal.ZERO
}

data class Invoice(
    val id: String,
    val number: String,
    val customerId: String,
    val invoiceDateMillis: Long,
    val dueDateMillis: Long?,
    val status: InvoiceStatus,
    val subtotal: BigDecimal,
    val discountTotal: BigDecimal,
    val taxTotal: BigDecimal,
    val grandTotal: BigDecimal,
    val paidAmount: BigDecimal,
    val remarks: String,
    val createdAtMillis: Long?,
    val updatedAtMillis: Long?,
    val createdBy: String,
    val updatedBy: String,
    val issuedAtMillis: Long?,
    val issuedBy: String,
    val cancelledAtMillis: Long?,
    val cancelledBy: String,
    val lines: List<InvoiceLine> = emptyList(),
) {
    init {
        require(dueDateMillis == null || dueDateMillis >= invoiceDateMillis)
        require(paidAmount >= BigDecimal.ZERO && paidAmount <= grandTotal)
    }

    val outstandingAmount: BigDecimal get() = grandTotal.subtract(paidAmount)
    val paymentStatus: InvoicePaymentStatus get() = InvoicePaymentStatus.from(paidAmount, grandTotal)

    fun isOverdue(currentDateMillis: Long): Boolean =
        status == InvoiceStatus.ISSUED && dueDateMillis != null && currentDateMillis > dueDateMillis && outstandingAmount > BigDecimal.ZERO
}

data class InvoiceLine(
    val id: String,
    val materialId: String,
    val description: String,
    val quantity: BigDecimal,
    val unit: String,
    val unitPrice: BigDecimal,
    val discountPercent: BigDecimal,
    val taxPercent: BigDecimal,
    val lineSubtotal: BigDecimal,
    val lineDiscount: BigDecimal,
    val taxableAmount: BigDecimal,
    val lineTax: BigDecimal,
    val lineTotal: BigDecimal,
    val sortOrder: Int,
)

enum class InvoiceStatus {
    DRAFT,
    ISSUED,
    CANCELLED;

    fun canTransitionTo(target: InvoiceStatus): Boolean = when (this) {
        DRAFT -> target == ISSUED || target == CANCELLED
        ISSUED -> target == CANCELLED
        CANCELLED -> false
    }
}

enum class InvoicePaymentStatus {
    UNPAID,
    PARTIALLY_PAID,
    PAID;

    companion object {
        fun from(paidAmount: BigDecimal, grandTotal: BigDecimal): InvoicePaymentStatus = when {
            paidAmount.compareTo(BigDecimal.ZERO) == 0 -> UNPAID
            paidAmount.compareTo(grandTotal) == 0 -> PAID
            else -> PARTIALLY_PAID
        }
    }
}
