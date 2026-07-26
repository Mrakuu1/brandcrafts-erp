package com.brandcrafts.erp.data.datasource.invoice

import com.brandcrafts.erp.core.result.InvoiceError
import com.brandcrafts.erp.core.result.InvoiceFailure
import com.brandcrafts.erp.data.repository.InvoiceActorValidator
import com.brandcrafts.erp.data.repository.InvoiceCreateIdGenerator
import com.brandcrafts.erp.data.repository.InvoiceCreateMapBuilder
import com.brandcrafts.erp.data.repository.InvoiceWritePolicy
import com.brandcrafts.erp.data.repository.PreparedInvoiceActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreInvoiceCancellationRemoteDataSource @Inject constructor(private val firestore: FirebaseFirestore, private val actorValidator: InvoiceActorValidator, private val ids: InvoiceCreateIdGenerator) : InvoiceCancellationRemoteDataSource {
    override suspend fun cancelInvoice(invoiceId: String) {
        if (invoiceId.isBlank()) throw InvoiceFailure(InvoiceError.InvoiceNotFound)
        val actor = actorValidator.requireAdmin(); InvoiceWritePolicy.validateCancellation(); val parent = firestore.collection(INVOICES).document(invoiceId)
        firestore.runTransaction { tx -> val current = tx.get(parent); if (!current.exists()) throw InvoiceFailure(InvoiceError.InvoiceNotFound); val status=current.getString(STATUS); val paid=try { java.math.BigDecimal(current.getString(PAID) ?: throw InvoiceFailure(InvoiceError.MalformedStoredDecimal)) } catch (_: NumberFormatException) { throw InvoiceFailure(InvoiceError.MalformedStoredDecimal) }; val payment=current.getString(PAYMENT); if (status !in setOf(DRAFT, ISSUED)) throw InvoiceFailure(InvoiceError.InvalidStatusTransition); if (paid > java.math.BigDecimal.ZERO) throw InvoiceFailure(InvoiceError.InvoiceHasRecordedPayments); if (paid < java.math.BigDecimal.ZERO || payment != UNPAID) throw InvoiceFailure(InvoiceError.InvalidPaidAmount); val number=current.getString(NUMBER).orEmpty(); if(number.isBlank())throw InvoiceFailure(InvoiceError.InvalidStoredStatus); tx.update(parent,mapOf(STATUS to CANCELLED,"cancelledAt" to FieldValue.serverTimestamp(),"cancelledBy" to actor.userId,"updatedAt" to FieldValue.serverTimestamp(),"updatedBy" to actor.userId)); val activity=PreparedInvoiceActivity(ids.generateActivityId(),"INVOICE_CANCELLED",invoiceId,actor.userId,actor.displayName,number);tx.set(firestore.collection(ACTIVITY).document(activity.activityId),InvoiceCreateMapBuilder.activity(activity)) }.awaitCancel()
    }
    private companion object { const val INVOICES="invoices";const val ACTIVITY="activity_logs";const val STATUS="status";const val PAID="paidAmount";const val PAYMENT="paymentStatus";const val NUMBER="invoiceNumber";const val DRAFT="DRAFT";const val ISSUED="ISSUED";const val CANCELLED="CANCELLED";const val UNPAID="UNPAID" }
}
private suspend fun <T> Task<T>.awaitCancel():T=suspendCancellableCoroutine{c->addOnSuccessListener{c.resume(it)}.addOnFailureListener{c.resumeWithException(it)}}
