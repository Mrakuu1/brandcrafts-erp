package com.brandcrafts.erp.data.datasource.invoice

import com.brandcrafts.erp.core.result.InvoiceError
import com.brandcrafts.erp.core.result.InvoiceFailure
import com.brandcrafts.erp.data.repository.ExistingInvoiceDraftSnapshot
import com.brandcrafts.erp.data.repository.ExistingInvoiceLineSnapshot
import com.brandcrafts.erp.data.repository.InvoiceDraftUpdateMapBuilder
import com.brandcrafts.erp.data.repository.InvoiceDraftUpdatePreparer
import com.brandcrafts.erp.domain.model.InvoiceDraftUpdateRequest
import com.brandcrafts.erp.domain.model.InvoicePaymentStatus
import com.brandcrafts.erp.domain.model.InvoiceStatus
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreInvoiceDraftUpdateRemoteDataSource @Inject constructor(private val firestore: FirebaseFirestore, private val preparer: InvoiceDraftUpdatePreparer) : InvoiceDraftUpdateRemoteDataSource {
    override suspend fun updateDraftInvoice(request: InvoiceDraftUpdateRequest) {
        if (request.invoiceId.isBlank()) throw InvoiceFailure(InvoiceError.InvoiceNotFound)
        val parent = firestore.collection(INVOICES).document(request.invoiceId)
        val snapshot = parent.get().awaitUpdate(); if (!snapshot.exists()) throw InvoiceFailure(InvoiceError.InvoiceNotFound)
        val lines = parent.collection(ITEMS).orderBy(SORT).get().awaitUpdate().documents.map { ExistingInvoiceLineSnapshot(it.id, it.getLong(SORT)?.toInt() ?: throw InvoiceFailure(InvoiceError.InvalidLine)) }
        val existing = ExistingInvoiceDraftSnapshot(snapshot.id, snapshot.getString(NUMBER).orEmpty(), InvoiceStatus.entries.firstOrNull { it.name == snapshot.getString(STATUS) } ?: throw InvoiceFailure(InvoiceError.InvalidStoredStatus), java.math.BigDecimal(snapshot.getString(PAID) ?: throw InvoiceFailure(InvoiceError.MalformedStoredDecimal)), InvoicePaymentStatus.entries.firstOrNull { it.name == snapshot.getString(PAYMENT) } ?: throw InvoiceFailure(InvoiceError.InvalidPaidAmount), snapshot.getTimestamp(CREATED)?.toDate()?.time, snapshot.getString(CREATED_BY).orEmpty(), lines)
        val prepared = preparer.prepare(request, existing)
        firestore.runTransaction { tx -> val current = tx.get(parent); if (!current.exists() || current.getString(STATUS) != InvoiceStatus.DRAFT.name || current.getString(PAID) != "0.00" || current.getString(PAYMENT) != InvoicePaymentStatus.UNPAID.name) throw InvoiceFailure(InvoiceError.DraftOnlyUpdateRequired); tx.update(parent, InvoiceDraftUpdateMapBuilder.parent(prepared.parent)); prepared.submittedLines.forEach { tx.set(parent.collection(ITEMS).document(it.lineId), InvoiceDraftUpdateMapBuilder.line(it)) }; prepared.staleLineIds.forEach { tx.delete(parent.collection(ITEMS).document(it)) }; tx.set(firestore.collection(ACTIVITY).document(prepared.activity.activityId), InvoiceDraftUpdateMapBuilder.activity(prepared.activity)) }.awaitUpdate()
    }
    private companion object { const val INVOICES="invoices"; const val ITEMS="items"; const val ACTIVITY="activity_logs"; const val NUMBER="invoiceNumber"; const val STATUS="status"; const val PAID="paidAmount"; const val PAYMENT="paymentStatus"; const val CREATED="createdAt"; const val CREATED_BY="createdBy"; const val SORT="sortOrder" }
}
private suspend fun <T> Task<T>.awaitUpdate(): T = suspendCancellableCoroutine { c -> addOnSuccessListener { c.resume(it) }.addOnFailureListener { c.resumeWithException(it) } }
