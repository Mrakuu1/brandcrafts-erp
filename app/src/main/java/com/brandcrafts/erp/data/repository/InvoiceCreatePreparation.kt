package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.core.result.InvoiceError
import com.brandcrafts.erp.core.result.InvoiceFailure
import com.brandcrafts.erp.domain.model.InvoicePaymentStatus
import com.brandcrafts.erp.domain.model.InvoiceStatus
import java.math.BigDecimal

data class PreparedInvoiceParent(
    val invoiceId: String,
    val customerId: String,
    val invoiceDateMillis: Long,
    val dueDateMillis: Long?,
    val status: InvoiceStatus,
    val subtotal: BigDecimal,
    val discountTotal: BigDecimal,
    val taxTotal: BigDecimal,
    val grandTotal: BigDecimal,
    val paidAmount: BigDecimal,
    val paymentStatus: InvoicePaymentStatus,
    val remarks: String,
    val createdBy: String,
    val updatedBy: String,
)

data class PreparedInvoiceLine(
    val lineId: String,
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

data class PreparedInvoiceActivity(
    val activityId: String,
    val action: String,
    val invoiceId: String,
    val actorId: String,
    val actorDisplayName: String,
    val invoiceNumber: String? = null,
)

data class InvoiceCreatePreparation(
    val actor: InvoiceValidatedActor,
    val customer: InvoiceValidatedCustomer,
    val parent: PreparedInvoiceParent,
    val lines: List<PreparedInvoiceLine>,
    val activity: PreparedInvoiceActivity,
    val itemCount: Int,
    val requestedWriteCount: Int,
) {
    fun finalize(invoiceNumber: String): FinalizedInvoiceCreateData {
        if (!InvoiceCounter.isValidNumber(invoiceNumber)) throw InvoiceFailure(InvoiceError.InvalidGeneratedInvoiceNumber)
        return FinalizedInvoiceCreateData(invoiceNumber, parent, lines, activity.copy(invoiceNumber = invoiceNumber), itemCount, requestedWriteCount)
    }
}

data class FinalizedInvoiceCreateData(
    val invoiceNumber: String,
    val parent: PreparedInvoiceParent,
    val lines: List<PreparedInvoiceLine>,
    val activity: PreparedInvoiceActivity,
    val itemCount: Int,
    val requestedWriteCount: Int,
)
