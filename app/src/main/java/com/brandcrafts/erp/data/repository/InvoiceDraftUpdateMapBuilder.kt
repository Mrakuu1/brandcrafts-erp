package com.brandcrafts.erp.data.repository

import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import java.util.Date

internal object InvoiceDraftUpdateMapBuilder {
    fun parent(parent: PreparedInvoiceDraftUpdateParent): Map<String, Any?> = mapOf("customerId" to parent.customerId, "invoiceDate" to Timestamp(Date(parent.invoiceDateMillis)), "dueDate" to parent.dueDateMillis?.let { Timestamp(Date(it)) }, "subtotal" to parent.subtotal.toPlainString(), "discountTotal" to parent.discountTotal.toPlainString(), "taxTotal" to parent.taxTotal.toPlainString(), "grandTotal" to parent.grandTotal.toPlainString(), "remarks" to parent.remarks, "updatedBy" to parent.updatedBy, "updatedAt" to FieldValue.serverTimestamp())
    fun line(line: PreparedInvoiceLine): Map<String, Any> = InvoiceCreateMapBuilder.line(line)
    fun activity(activity: PreparedInvoiceActivity): Map<String, Any> = InvoiceCreateMapBuilder.activity(activity)
}
