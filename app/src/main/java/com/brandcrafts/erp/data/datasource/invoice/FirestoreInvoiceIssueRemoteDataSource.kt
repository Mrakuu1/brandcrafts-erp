package com.brandcrafts.erp.data.datasource.invoice

import com.brandcrafts.erp.core.result.InvoiceError
import com.brandcrafts.erp.core.result.InvoiceFailure
import com.brandcrafts.erp.data.repository.InvoiceActorValidator
import com.brandcrafts.erp.data.repository.InvoiceCreateIdGenerator
import com.brandcrafts.erp.data.repository.InvoiceCreateMapBuilder
import com.brandcrafts.erp.data.repository.InvoiceCustomerValidator
import com.brandcrafts.erp.data.repository.InvoiceDraftUpdateMapBuilder
import com.brandcrafts.erp.data.repository.PreparedInvoiceActivity
import com.brandcrafts.erp.data.repository.InvoiceWritePolicy
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreInvoiceIssueRemoteDataSource @Inject constructor(private val firestore: FirebaseFirestore, private val actorValidator: InvoiceActorValidator, private val customerValidator: InvoiceCustomerValidator, private val ids: InvoiceCreateIdGenerator) : InvoiceIssueRemoteDataSource {
    override suspend fun issueInvoice(invoiceId: String) {
        if (invoiceId.isBlank()) throw InvoiceFailure(InvoiceError.InvoiceNotFound)
        val actor = actorValidator.requireAdmin(); InvoiceWritePolicy.validateIssue()
        val parent = firestore.collection(INVOICES).document(invoiceId); val initial = parent.get().awaitIssue()
        if (!initial.exists()) throw InvoiceFailure(InvoiceError.InvoiceNotFound)
        customerValidator.requireActiveCustomer(initial.getString(CUSTOMER).orEmpty())
        firestore.runTransaction { tx -> val current = tx.get(parent); val number = current.getString(NUMBER).orEmpty(); val total = current.getString(TOTAL); if (!current.exists() || current.getString(STATUS) != DRAFT || current.getString(PAID) != "0.00" || current.getString(PAYMENT) != UNPAID || current.getTimestamp(DATE) == null || number.isBlank()) throw InvoiceFailure(InvoiceError.InvalidStatusTransition); try { java.math.BigDecimal(total ?: throw InvoiceFailure(InvoiceError.MalformedStoredDecimal)) } catch (_: NumberFormatException) { throw InvoiceFailure(InvoiceError.MalformedStoredDecimal) }; tx.update(parent, mapOf(STATUS to ISSUED, "issuedAt" to FieldValue.serverTimestamp(), "issuedBy" to actor.userId, "updatedAt" to FieldValue.serverTimestamp(), "updatedBy" to actor.userId)); val activity=PreparedInvoiceActivity(ids.generateActivityId(),"INVOICE_ISSUED",invoiceId,actor.userId,actor.displayName,number); tx.set(firestore.collection(ACTIVITY).document(activity.activityId),InvoiceCreateMapBuilder.activity(activity)) }.awaitIssue()
    }
    private companion object { const val INVOICES="invoices"; const val ACTIVITY="activity_logs"; const val CUSTOMER="customerId"; const val NUMBER="invoiceNumber"; const val STATUS="status"; const val PAID="paidAmount"; const val PAYMENT="paymentStatus"; const val TOTAL="grandTotal"; const val DATE="invoiceDate"; const val DRAFT="DRAFT"; const val ISSUED="ISSUED"; const val UNPAID="UNPAID" }
}
private suspend fun <T> Task<T>.awaitIssue():T=suspendCancellableCoroutine{c->addOnSuccessListener{c.resume(it)}.addOnFailureListener{c.resumeWithException(it)}}
