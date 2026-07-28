package com.brandcrafts.erp.data.datasource.invoice

import com.brandcrafts.erp.core.result.InvoiceError
import com.brandcrafts.erp.core.result.InvoiceFailure
import com.brandcrafts.erp.data.model.invoice.InvoiceDto
import com.brandcrafts.erp.data.model.invoice.InvoiceLineDto
import com.brandcrafts.erp.data.mapper.toSummaryDomain
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreInvoiceRemoteDataSource @Inject constructor(private val firestore: FirebaseFirestore) : InvoiceRemoteDataSource {
    override fun observeInvoiceParents() = callbackFlow {
        // Parent-only listener. Local ordering keeps legacy invoices without a
        // Firestore Timestamp from being hidden by an orderBy query.
        val registration = firestore.collection(INVOICES)
            .addSnapshotListener { snapshot, error ->
                if (error != null) close(mapFailure(error))
                else if (snapshot != null) try {
                    trySend(
                        snapshot.documents
                            .map(::parentDto)
                            .map { it.toSummaryDomain() }
                            .sortedByDescending { it.invoiceDateMillis },
                    )
                } catch (failure: Throwable) { close(failure) }
            }
        awaitClose(registration::remove)
    }

    override suspend fun getInvoiceParent(invoiceId: String): InvoiceDto? {
        requireId(invoiceId)
        return firestore.collection(INVOICES).document(invoiceId).get().awaitInvoice().let { if (it.exists()) parentDto(it) else null }
    }

    override suspend fun getInvoiceLines(invoiceId: String): List<InvoiceLineDto> {
        requireId(invoiceId)
        return firestore.collection(INVOICES).document(invoiceId).collection(ITEMS).orderBy(SORT_ORDER)
            .get().awaitInvoice().documents.map(::lineDto)
    }

    private fun parentDto(d: DocumentSnapshot) = InvoiceDto(d.id, d.getString(NUMBER), d.getString(CUSTOMER), d.get(INVOICE_DATE), d.get(DUE_DATE), d.getString(STATUS), decimalText(d, SUBTOTAL), decimalText(d, DISCOUNT), decimalText(d, TAX), decimalText(d, GRAND_TOTAL), decimalText(d, PAID), d.getString(PAYMENT_STATUS), d.getString(REMARKS), d.getTimestamp(CREATED_AT), d.getString(CREATED_BY), d.getTimestamp(UPDATED_AT), d.getString(UPDATED_BY), d.getTimestamp(ISSUED_AT), d.getString(ISSUED_BY), d.getTimestamp(CANCELLED_AT), d.getString(CANCELLED_BY))
    private fun lineDto(d: DocumentSnapshot) = InvoiceLineDto(d.id, d.getString(MATERIAL), d.getString(DESCRIPTION), d.getString(QUANTITY), d.getString(UNIT), d.getString(UNIT_PRICE), d.getString(DISCOUNT_PERCENT), d.getString(TAX_PERCENT), d.getString(LINE_SUBTOTAL), d.getString(LINE_DISCOUNT), d.getString(TAXABLE), d.getString(LINE_TAX), d.getString(LINE_TOTAL), d.getLong(SORT_ORDER)?.toInt())
    private fun requireId(id: String) { if (id.isBlank()) throw InvoiceFailure(InvoiceError.InvoiceNotFound) }
    private fun decimalText(document: DocumentSnapshot, field: String): String? = when (val value = document.get(field)) {
        null -> null
        is String -> value
        is Number -> value.toString()
        else -> throw InvoiceFailure(InvoiceError.MalformedStoredDecimal)
    }
    private fun mapFailure(error: Throwable): Throwable = when (error as? FirebaseFirestoreException) {
        null -> InvoiceFailure(InvoiceError.RepositoryUnavailable)
        else -> InvoiceFailure(
            when (error.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> InvoiceError.PermissionDenied
                FirebaseFirestoreException.Code.UNAVAILABLE -> InvoiceError.FirestoreUnavailable
                else -> InvoiceError.RepositoryUnavailable
            },
        )
    }

    private companion object { const val INVOICES="invoices"; const val ITEMS="items"; const val INVOICE_DATE="invoiceDate"; const val DUE_DATE="dueDate"; const val NUMBER="invoiceNumber"; const val CUSTOMER="customerId"; const val STATUS="status"; const val SUBTOTAL="subtotal"; const val DISCOUNT="discountTotal"; const val TAX="taxTotal"; const val GRAND_TOTAL="grandTotal"; const val PAID="paidAmount"; const val PAYMENT_STATUS="paymentStatus"; const val REMARKS="remarks"; const val CREATED_AT="createdAt"; const val CREATED_BY="createdBy"; const val UPDATED_AT="updatedAt"; const val UPDATED_BY="updatedBy"; const val ISSUED_AT="issuedAt"; const val ISSUED_BY="issuedBy"; const val CANCELLED_AT="cancelledAt"; const val CANCELLED_BY="cancelledBy"; const val MATERIAL="materialId"; const val DESCRIPTION="description"; const val QUANTITY="quantity"; const val UNIT="unit"; const val UNIT_PRICE="unitPrice"; const val DISCOUNT_PERCENT="discountPercent"; const val TAX_PERCENT="taxPercent"; const val LINE_SUBTOTAL="lineSubtotal"; const val LINE_DISCOUNT="lineDiscount"; const val TAXABLE="taxableAmount"; const val LINE_TAX="lineTax"; const val LINE_TOTAL="lineTotal"; const val SORT_ORDER="sortOrder" }
}

private suspend fun <T> Task<T>.awaitInvoice(): T = suspendCancellableCoroutine { continuation -> addOnSuccessListener { continuation.resume(it) }.addOnFailureListener { continuation.resumeWithException(it) } }
