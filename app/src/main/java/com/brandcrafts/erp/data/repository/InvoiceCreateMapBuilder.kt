package com.brandcrafts.erp.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import java.util.Date

internal object InvoiceCreateMapBuilder {
    fun counter(nextValue: Long): Map<String, Any> = mapOf("nextNumber" to nextValue, "prefix" to "INV")

    fun parent(data: FinalizedInvoiceCreateData): Map<String, Any?> = with(data.parent) {
        mapOf(
            "id" to invoiceId, "invoiceNumber" to data.invoiceNumber, "customerId" to customerId,
            "invoiceDate" to Timestamp(Date(invoiceDateMillis)), "dueDate" to dueDateMillis?.let { Timestamp(Date(it)) },
            "status" to status.name, "subtotal" to subtotal.toPlainString(), "discountTotal" to discountTotal.toPlainString(),
            "taxTotal" to taxTotal.toPlainString(), "grandTotal" to grandTotal.toPlainString(),
            "paidAmount" to paidAmount.toPlainString(), "paymentStatus" to paymentStatus.name, "remarks" to remarks,
            "createdBy" to createdBy, "updatedBy" to updatedBy, "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp(),
        )
    }

    fun line(line: PreparedInvoiceLine): Map<String, Any> = mapOf(
        "itemId" to line.lineId, "materialId" to line.materialId, "description" to line.description,
        "quantity" to line.quantity.toPlainString(), "unit" to line.unit, "unitPrice" to line.unitPrice.toPlainString(),
        "discountPercent" to line.discountPercent.toPlainString(), "taxPercent" to line.taxPercent.toPlainString(),
        "lineSubtotal" to line.lineSubtotal.toPlainString(), "lineDiscount" to line.lineDiscount.toPlainString(),
        "taxableAmount" to line.taxableAmount.toPlainString(), "lineTax" to line.lineTax.toPlainString(),
        "lineTotal" to line.lineTotal.toPlainString(), "sortOrder" to line.sortOrder,
    )

    fun activity(activity: PreparedInvoiceActivity): Map<String, Any> = mapOf(
        "id" to activity.activityId, "module" to "INVOICES", "action" to activity.action,
        "referenceId" to activity.invoiceId, "referenceType" to "INVOICE", "invoiceNumber" to requireNotNull(activity.invoiceNumber),
        "description" to activity.action, "performedBy" to activity.actorId, "performedByName" to activity.actorDisplayName,
        "createdAt" to FieldValue.serverTimestamp(),
    )
}
