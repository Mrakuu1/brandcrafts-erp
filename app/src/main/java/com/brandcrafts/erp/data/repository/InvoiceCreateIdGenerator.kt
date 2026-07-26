package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.core.result.InvoiceError
import com.brandcrafts.erp.core.result.InvoiceFailure
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject

data class InvoiceCreateIds(
    val invoiceId: String,
    val lineIds: List<String>,
    val activityId: String,
)

class InvoiceCreateIdGenerator @Inject constructor(
    private val firestore: FirebaseFirestore,
) {
    fun generateCreateIds(lineCount: Int): InvoiceCreateIds {
        if (lineCount < 0) throw InvoiceFailure(InvoiceError.InvalidOperationCount)
        val invoice = firestore.collection(INVOICES).document()
        val lineIds = List(lineCount) { invoice.collection(ITEMS).document().id }
        val activityId = firestore.collection(ACTIVITY_LOGS).document().id
        validate(invoice.id, lineIds, activityId)
        return InvoiceCreateIds(invoice.id, lineIds, activityId)
    }
    fun generateLineIds(invoiceId: String, count: Int): List<String> {
        if (invoiceId.isBlank() || count < 0) throw InvoiceFailure(InvoiceError.InvalidGeneratedDocumentId)
        return List(count) { firestore.collection(INVOICES).document(invoiceId).collection(ITEMS).document().id }
            .also { validate(invoiceId, it, firestore.collection(ACTIVITY_LOGS).document().id) }
    }
    fun generateActivityId(): String = firestore.collection(ACTIVITY_LOGS).document().id.also { if (it.isBlank()) throw InvoiceFailure(InvoiceError.InvalidGeneratedDocumentId) }

    private fun validate(invoiceId: String, lineIds: List<String>, activityId: String) {
        if (invoiceId.isBlank() || activityId.isBlank() || lineIds.any(String::isBlank)) {
            throw InvoiceFailure(InvoiceError.InvalidGeneratedDocumentId)
        }
        if (lineIds.toSet().size != lineIds.size || invoiceId in lineIds || activityId == invoiceId || activityId in lineIds) {
            throw InvoiceFailure(InvoiceError.InvalidGeneratedDocumentId)
        }
    }

    private companion object {
        const val INVOICES = "invoices"
        const val ITEMS = "items"
        const val ACTIVITY_LOGS = "activity_logs"
    }
}
