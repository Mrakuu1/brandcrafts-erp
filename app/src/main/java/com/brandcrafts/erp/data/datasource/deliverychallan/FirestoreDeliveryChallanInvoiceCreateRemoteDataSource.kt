package com.brandcrafts.erp.data.datasource.deliverychallan

import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.core.result.DeliveryChallanError
import com.brandcrafts.erp.core.result.DeliveryChallanFailure
import com.brandcrafts.erp.data.repository.DeliveryChallanActivityFactory
import com.brandcrafts.erp.data.repository.DeliveryChallanCounter
import com.brandcrafts.erp.data.repository.DeliveryChallanWritePolicy
import com.brandcrafts.erp.domain.model.DeliveryChallanInvoiceCreateRequest
import com.brandcrafts.erp.domain.model.DeliveryChallanSourceType
import com.brandcrafts.erp.domain.model.DeliveryChallanStatus
import com.brandcrafts.erp.domain.model.UserRole
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.Timestamp
import java.math.BigDecimal
import java.util.Date
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreDeliveryChallanInvoiceCreateRemoteDataSource constructor(
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager,
) : DeliveryChallanInvoiceCreateRemoteDataSource {
    override suspend fun createFromInvoice(request: DeliveryChallanInvoiceCreateRequest): String {
        val actor = (sessionManager.currentUser.value as? CurrentUserState.Authenticated)?.user
            ?: throw DeliveryChallanFailure(DeliveryChallanError.Unauthenticated)
        if (!actor.active) throw DeliveryChallanFailure(DeliveryChallanError.InactiveUser)
        if (actor.uid.isBlank()) throw DeliveryChallanFailure(DeliveryChallanError.UserProfileMissing)
        if (actor.role != UserRole.ADMIN) throw DeliveryChallanFailure(DeliveryChallanError.PermissionDenied)
        if (request.invoiceId.isBlank() || request.deliveryAddress.isBlank() || request.dateMillis <= 0L || request.lines.isEmpty() || request.lines.any { it.sourceInvoiceLineId.isBlank() || it.quantity <= BigDecimal.ZERO }) throw DeliveryChallanFailure(DeliveryChallanError.InvalidInvoiceSource)
        if (request.lines.map { it.sourceInvoiceLineId }.toSet().size != request.lines.size) throw DeliveryChallanFailure(DeliveryChallanError.InvalidLineId)
        DeliveryChallanWritePolicy.validate(DeliveryChallanWritePolicy.createWriteCount(request.lines.size))
        val parent = firestore.collection(CHALLANS).document(); val itemIds = List(request.lines.size) { parent.collection(ITEMS).document().id }; val activityId = firestore.collection(ACTIVITY).document().id
        val invoice = firestore.collection(INVOICES).document(request.invoiceId)
        val sourceItems = invoice.collection(ITEMS).orderBy(SORT_ORDER).get().awaitInvoiceDc()
        val activeChallans = firestore.collection(CHALLANS)
            .whereEqualTo(SOURCE_INVOICE_ID, request.invoiceId)
            .whereIn(STATUS, listOf(DRAFT, DISPATCHED))
            .get()
            .awaitInvoiceDc()
        if (!activeChallans.isEmpty) {
            throw DeliveryChallanFailure(DeliveryChallanError.InvoiceAlreadyHasActiveDeliveryChallan)
        }
        try {
            firestore.runTransaction { tx ->
                val counter = firestore.document(DeliveryChallanCounter.COUNTER_DOCUMENT_PATH)
                val invoiceSnapshot = tx.get(invoice); val counterSnapshot = tx.get(counter)
                if (!invoiceSnapshot.exists() || invoiceSnapshot.getString(STATUS) != ISSUED) throw DeliveryChallanFailure(DeliveryChallanError.InvalidInvoiceSource)
                val customerId = invoiceSnapshot.getString(CUSTOMER)?.takeIf(String::isNotBlank) ?: throw DeliveryChallanFailure(DeliveryChallanError.InvalidInvoiceSource)
                val invoiceNumber = invoiceSnapshot.getString(INVOICE_NUMBER)?.takeIf(String::isNotBlank) ?: throw DeliveryChallanFailure(DeliveryChallanError.InvalidInvoiceSource)
                val sourceById = sourceItems.documents.associateBy { it.id }
                val copied = request.lines.map { requested ->
                    val source = sourceById[requested.sourceInvoiceLineId] ?: throw DeliveryChallanFailure(DeliveryChallanError.InvalidInvoiceSource)
                    val available = try { BigDecimal(source.getString(QUANTITY) ?: throw DeliveryChallanFailure(DeliveryChallanError.InvalidInvoiceSource)) } catch (_: NumberFormatException) { throw DeliveryChallanFailure(DeliveryChallanError.InvalidInvoiceSource) }
                    if (requested.quantity > available) throw DeliveryChallanFailure(DeliveryChallanError.InvoiceQuantityExceeded)
                    CopiedLine(source.getString(MATERIAL).orEmpty(), source.getString(DESCRIPTION).orEmpty(), requested.quantity, source.getString(UNIT).orEmpty())
                }
                if (copied.any { it.materialId.isBlank() || it.description.isBlank() || it.unit.isBlank() }) throw DeliveryChallanFailure(DeliveryChallanError.InvalidInvoiceSource)
                val number = DeliveryChallanCounter.format(DeliveryChallanCounter.nextValue(DeliveryChallanCounter.currentValue(counterSnapshot.get("value"))))
                tx.set(counter, mapOf("value" to DeliveryChallanCounter.nextValue(DeliveryChallanCounter.currentValue(counterSnapshot.get("value")))))
                tx.set(parent, mapOf("id" to parent.id, "dcNumber" to number, "customerId" to customerId, "deliveryAddress" to request.deliveryAddress.trim(), "date" to Timestamp(Date(request.dateMillis)), "sourceType" to DeliveryChallanSourceType.INVOICE.name, "sourceInvoiceId" to request.invoiceId, "sourceInvoiceNumber" to invoiceNumber, "vehicleNumber" to request.vehicleNumber.trim(), "driverName" to request.driverName.trim(), "notes" to request.notes.trim(), "status" to DRAFT, "createdBy" to actor.uid, "updatedBy" to actor.uid, "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp()))
                copied.forEachIndexed { index, line -> tx.set(parent.collection(ITEMS).document(itemIds[index]), mapOf("itemId" to itemIds[index], "materialId" to line.materialId, "description" to line.description, "quantity" to line.quantity.toPlainString(), "unit" to line.unit, "sortOrder" to index)) }
                val activity = DeliveryChallanActivityFactory.created(activityId, parent.id, number, actor.uid, actor.name)
                tx.set(firestore.collection(ACTIVITY).document(activityId), mapOf("id" to activityId, "module" to "DELIVERY_CHALLAN", "action" to activity.action, "referenceId" to parent.id, "referenceType" to "DELIVERY_CHALLAN", "description" to number, "performedBy" to actor.uid, "performedByName" to actor.name, "createdAt" to FieldValue.serverTimestamp()))
            }.awaitInvoiceDc()
            return parent.id
        } catch (exception: CancellationException) { throw exception } catch (failure: DeliveryChallanFailure) { throw failure } catch (_: Throwable) { throw DeliveryChallanFailure(DeliveryChallanError.Unknown) }
    }
    private data class CopiedLine(val materialId: String, val description: String, val quantity: BigDecimal, val unit: String)
    private companion object { const val CHALLANS="delivery_challans"; const val INVOICES="invoices"; const val ITEMS="items"; const val ACTIVITY="activity_logs"; const val STATUS="status"; const val DRAFT="DRAFT"; const val DISPATCHED="DISPATCHED"; const val ISSUED="ISSUED"; const val SOURCE_INVOICE_ID="sourceInvoiceId"; const val INVOICE_NUMBER="invoiceNumber"; const val CUSTOMER="customerId"; const val MATERIAL="materialId"; const val DESCRIPTION="description"; const val QUANTITY="quantity"; const val UNIT="unit"; const val SORT_ORDER="sortOrder" }
}
private suspend fun <T> Task<T>.awaitInvoiceDc(): T = suspendCancellableCoroutine { continuation -> addOnSuccessListener { continuation.resume(it) }.addOnFailureListener { continuation.resumeWithException(it) } }
