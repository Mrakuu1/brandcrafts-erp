package com.brandcrafts.erp.data.datasource.invoice

import com.brandcrafts.erp.core.result.InvoiceError
import com.brandcrafts.erp.core.result.InvoiceFailure
import com.brandcrafts.erp.data.repository.InvoiceActorValidator
import com.brandcrafts.erp.data.repository.InvoiceCreateIdGenerator
import com.brandcrafts.erp.data.repository.InvoiceCreateMapBuilder
import com.brandcrafts.erp.data.repository.InvoiceWritePolicy
import com.brandcrafts.erp.data.repository.PreparedInvoiceActivity
import com.brandcrafts.erp.domain.model.Invoice
import com.brandcrafts.erp.domain.model.InvoicePaymentRequest
import com.brandcrafts.erp.domain.model.InvoiceStatus
import com.brandcrafts.erp.domain.usecase.invoice.InvoiceValidator
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreInvoicePaymentRemoteDataSource @Inject constructor(private val firestore: FirebaseFirestore, private val actorValidator: InvoiceActorValidator, private val validator: InvoiceValidator, private val ids: InvoiceCreateIdGenerator) : InvoicePaymentRemoteDataSource {
    override suspend fun recordPayment(request: InvoicePaymentRequest) {
        if (request.invoiceId.isBlank() || request.amount <= java.math.BigDecimal.ZERO) throw InvoiceFailure(InvoiceError.InvalidPaidAmount)
        val actor=actorValidator.requireAdmin();InvoiceWritePolicy.validatePayment();val parent=firestore.collection(INVOICES).document(request.invoiceId)
        firestore.runTransaction { tx -> val s=tx.get(parent);if(!s.exists())throw InvoiceFailure(InvoiceError.InvoiceNotFound);val status=InvoiceStatus.entries.firstOrNull{it.name==s.getString(STATUS)}?:throw InvoiceFailure(InvoiceError.InvalidStoredStatus);val total=decimal(s.getString(TOTAL));val paid=decimal(s.getString(PAID));val payment=s.getString(PAYMENT);val invoice=Invoice(s.id,s.getString(NUMBER).orEmpty(),s.getString(CUSTOMER).orEmpty(),s.getTimestamp(DATE)?.toDate()?.time?:throw InvoiceFailure(InvoiceError.MalformedStoredDate),null,status,java.math.BigDecimal.ZERO,java.math.BigDecimal.ZERO,java.math.BigDecimal.ZERO,total,paid,"",null,null,"","",null,"",null,"",emptyList());if(payment!=invoice.paymentStatus.name)throw InvoiceFailure(InvoiceError.InvalidPaidAmount);val next=validator.validatePaymentRecording(invoice,request.amount).getOrElse{throw it};tx.update(parent,mapOf(PAID to next.paidAmount.toPlainString(),PAYMENT to next.paymentStatus.name,"updatedAt" to FieldValue.serverTimestamp(),"updatedBy" to actor.userId));val a=PreparedInvoiceActivity(ids.generateActivityId(),"INVOICE_PAYMENT_RECORDED",request.invoiceId,actor.userId,actor.displayName,s.getString(NUMBER));tx.set(firestore.collection(ACTIVITY).document(a.activityId),InvoiceCreateMapBuilder.activity(a)) }.awaitPayment()
    }
    private fun decimal(value:String?)=try{java.math.BigDecimal(value?:throw InvoiceFailure(InvoiceError.MalformedStoredDecimal))}catch(_:NumberFormatException){throw InvoiceFailure(InvoiceError.MalformedStoredDecimal)}
    private companion object{const val INVOICES="invoices";const val ACTIVITY="activity_logs";const val STATUS="status";const val TOTAL="grandTotal";const val PAID="paidAmount";const val PAYMENT="paymentStatus";const val NUMBER="invoiceNumber";const val CUSTOMER="customerId";const val DATE="invoiceDate"}
}
private suspend fun <T> Task<T>.awaitPayment():T=suspendCancellableCoroutine{c->addOnSuccessListener{c.resume(it)}.addOnFailureListener{c.resumeWithException(it)}}
