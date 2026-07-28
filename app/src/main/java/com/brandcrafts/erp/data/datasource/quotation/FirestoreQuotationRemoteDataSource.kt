package com.brandcrafts.erp.data.datasource.quotation

import com.brandcrafts.erp.domain.model.Quotation
import com.brandcrafts.erp.domain.model.QuotationStatus
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import java.math.BigDecimal
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

/** Observes quotation headers only; line items are loaded only for quotation details. */
class FirestoreQuotationRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) : QuotationRemoteDataSource {
    override fun observeQuotations() = callbackFlow {
        // Sorting locally avoids the type + date composite-index requirement and
        // keeps legacy epoch dates visible during the transition to Timestamps.
        val listener = firestore.collection(DOCUMENTS)
            .whereEqualTo(TYPE, QUOTATION)
            .addSnapshotListener { snapshot, error ->
                when {
                    error != null -> close(error)
                    snapshot != null -> try {
                        val quotations = snapshot.documents
                            .map(::toQuotation)
                            .sortedByDescending { it.dateMillis ?: Long.MIN_VALUE }
                        trySend(quotations)
                    } catch (exception: IllegalArgumentException) {
                        close(exception)
                    }
                }
            }
        awaitClose(listener::remove)
    }

    private fun toQuotation(document: DocumentSnapshot): Quotation = Quotation(
        id = document.id.takeIf(String::isNotBlank)
            ?: throw IllegalArgumentException("Quotation document ID is missing"),
        number = requiredText(document, DOCUMENT_NUMBER),
        contactId = requiredText(document, CONTACT_ID),
        dateMillis = businessDate(document.get(DATE), required = false),
        validUntilMillis = businessDate(document.get(VALID_UNTIL), required = false),
        status = QuotationStatus.entries.firstOrNull { it.name == document.getString(STATUS) }
            ?: throw IllegalArgumentException("Quotation status is invalid"),
        grandTotal = decimal(document.get(GRAND_TOTAL)),
        pdfUrl = document.getString(PDF_URL).orEmpty(),
        createdBy = document.getString(CREATED_BY).orEmpty(),
        remarks = document.getString(REMARKS).orEmpty(),
    )

    private fun requiredText(document: DocumentSnapshot, field: String): String =
        document.getString(field)?.trim()?.takeIf(String::isNotEmpty)
            ?: throw IllegalArgumentException("Quotation $field is missing")

    private fun decimal(value: Any?): BigDecimal = when (value) {
        is Number -> BigDecimal(value.toString())
        is String -> value.trim().takeIf(String::isNotEmpty)?.let(::BigDecimal)
        else -> null
    } ?: throw IllegalArgumentException("Quotation total is invalid")

    private fun businessDate(value: Any?, required: Boolean): Long? = when (value) {
        null -> if (required) throw IllegalArgumentException("Quotation date is missing") else null
        is Timestamp -> value.toDate().time
        is Date -> value.time
        is Number -> value.toLong().takeIf { it > 0 }
        is String -> value.toLongOrNull()?.takeIf { it > 0 }
        else -> null
    } ?: if (required) throw IllegalArgumentException("Quotation date is invalid") else null

    private companion object {
        const val DOCUMENTS = "documents"
        const val TYPE = "type"
        const val QUOTATION = "QUOTATION"
        const val DOCUMENT_NUMBER = "documentNumber"
        const val CONTACT_ID = "contactId"
        const val DATE = "date"
        const val VALID_UNTIL = "validUntil"
        const val STATUS = "status"
        const val GRAND_TOTAL = "grandTotal"
        const val PDF_URL = "pdfUrl"
        const val CREATED_BY = "createdBy"
        const val REMARKS = "remarks"
    }
}
