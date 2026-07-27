package com.brandcrafts.erp.data.datasource.deliverychallan

import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.core.result.DeliveryChallanError
import com.brandcrafts.erp.core.result.DeliveryChallanFailure
import com.brandcrafts.erp.data.repository.DeliveryChallanActivityFactory
import com.brandcrafts.erp.data.repository.DeliveryChallanWritePolicy
import com.brandcrafts.erp.domain.model.DeliveryChallanDraftUpdateRequest
import com.brandcrafts.erp.domain.model.DeliveryChallanSourceType
import com.brandcrafts.erp.domain.model.UserRole
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import java.math.BigDecimal
import java.util.Date
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreDeliveryChallanDraftUpdateRemoteDataSource constructor(
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager,
) : DeliveryChallanDraftUpdateRemoteDataSource {
    override suspend fun updateDraft(request: DeliveryChallanDraftUpdateRequest) {
        val actor = (sessionManager.currentUser.value as? CurrentUserState.Authenticated)?.user
            ?.takeIf { it.active && it.role == UserRole.ADMIN && it.uid.isNotBlank() }
            ?: throw DeliveryChallanFailure(DeliveryChallanError.PermissionDenied)
        if (request.challanId.isBlank() || request.customerId.isBlank() || request.deliveryAddress.isBlank() || request.dateMillis <= 0 || request.lines.isEmpty()) throw DeliveryChallanFailure(DeliveryChallanError.InvalidLine)
        val parent = firestore.collection(CHALLANS).document(request.challanId)
        val existingLines = parent.collection(ITEMS).get().awaitDraftDc().documents
        val existingIds = existingLines.map { it.id }.toSet()
        val retainedIds = request.lines.map { it.id }.filter(String::isNotBlank)
        if (retainedIds.toSet().size != retainedIds.size || !retainedIds.all(existingIds::contains)) throw DeliveryChallanFailure(DeliveryChallanError.InvalidLineId)
        val staleIds = existingIds - retainedIds.toSet()
        DeliveryChallanWritePolicy.validate(DeliveryChallanWritePolicy.updateWriteCount(request.lines.size, staleIds.size))
        val resolvedIds = request.lines.map { it.id.takeIf(String::isNotBlank) ?: parent.collection(ITEMS).document().id }
        val invoiceLineQuantities = if (request.sourceType == DeliveryChallanSourceType.INVOICE) {
            val invoiceId = request.sourceInvoiceId ?: throw DeliveryChallanFailure(DeliveryChallanError.InvalidInvoiceSource)
            firestore.collection(INVOICES).document(invoiceId).collection(ITEMS).get().awaitDraftDc().documents
                .groupBy { it.getString("materialId").orEmpty() }
                .mapValues { (_, lines) ->
                    lines.fold(BigDecimal.ZERO) { total, line ->
                        total + try {
                            BigDecimal(line.getString("quantity") ?: throw DeliveryChallanFailure(DeliveryChallanError.InvalidInvoiceSource))
                        } catch (_: NumberFormatException) {
                            throw DeliveryChallanFailure(DeliveryChallanError.InvalidInvoiceSource)
                        }
                    }
                }
        } else {
            emptyMap()
        }
        try {
            firestore.runTransaction { tx ->
                val current = tx.get(parent)
                if (!current.exists()) throw DeliveryChallanFailure(DeliveryChallanError.DeliveryChallanNotFound)
                if (current.getString(STATUS) != DRAFT) throw DeliveryChallanFailure(DeliveryChallanError.DraftOnlyUpdateRequired)
                val sourceType = current.getString(SOURCE_TYPE) ?: throw DeliveryChallanFailure(DeliveryChallanError.InvalidInvoiceSource)
                val number = current.getString(NUMBER)?.takeIf(String::isNotBlank) ?: throw DeliveryChallanFailure(DeliveryChallanError.InvalidLine)
                if (sourceType == DeliveryChallanSourceType.INVOICE.name) {
                    if (current.getString(SOURCE_INVOICE_ID) != request.sourceInvoiceId) {
                        throw DeliveryChallanFailure(DeliveryChallanError.InvalidInvoiceSource)
                    }
                    validateInvoiceLimits(invoiceLineQuantities, request)
                }
                tx.update(parent, mapOf("customerId" to current.getString("customerId"), "deliveryAddress" to request.deliveryAddress.trim(), "date" to Timestamp(Date(request.dateMillis)), "vehicleNumber" to request.vehicleNumber.trim(), "driverName" to request.driverName.trim(), "notes" to request.notes.trim(), "updatedBy" to actor.uid, "updatedAt" to FieldValue.serverTimestamp()))
                request.lines.forEachIndexed { index, line -> tx.set(parent.collection(ITEMS).document(resolvedIds[index]), mapOf("itemId" to resolvedIds[index], "materialId" to line.materialId, "description" to line.description, "quantity" to line.quantity.toPlainString(), "unit" to line.unit, "sortOrder" to index)) }
                staleIds.forEach { tx.delete(parent.collection(ITEMS).document(it)) }
                val activityId = firestore.collection(ACTIVITY).document().id; val activity = DeliveryChallanActivityFactory.updated(activityId, parent.id, number, actor.uid, actor.name)
                tx.set(firestore.collection(ACTIVITY).document(activityId), mapOf("id" to activityId, "module" to "DELIVERY_CHALLAN", "action" to activity.action, "referenceId" to parent.id, "referenceType" to "DELIVERY_CHALLAN", "description" to number, "performedBy" to actor.uid, "performedByName" to actor.name, "createdAt" to FieldValue.serverTimestamp()))
            }.awaitDraftDc()
        } catch (exception: CancellationException) { throw exception } catch (failure: DeliveryChallanFailure) { throw failure } catch (_: Throwable) { throw DeliveryChallanFailure(DeliveryChallanError.Unknown) }
    }
    private fun validateInvoiceLimits(
        availableByMaterial: Map<String, BigDecimal>,
        request: DeliveryChallanDraftUpdateRequest,
    ) {
        request.lines.groupBy { it.materialId }.forEach { (materialId, lines) ->
            val requested = lines.fold(BigDecimal.ZERO) { total, line -> total + line.quantity }
            if (materialId.isBlank() || requested > (availableByMaterial[materialId] ?: BigDecimal.ZERO)) {
                throw DeliveryChallanFailure(DeliveryChallanError.InvoiceQuantityExceeded)
            }
        }
    }
    private companion object { const val CHALLANS="delivery_challans"; const val INVOICES="invoices"; const val ITEMS="items"; const val ACTIVITY="activity_logs"; const val STATUS="status"; const val DRAFT="DRAFT"; const val ISSUED="ISSUED"; const val NUMBER="dcNumber"; const val SOURCE_TYPE="sourceType"; const val SOURCE_INVOICE_ID="sourceInvoiceId" }
}
private suspend fun <T> Task<T>.awaitDraftDc(): T = suspendCancellableCoroutine { continuation -> addOnSuccessListener { continuation.resume(it) }.addOnFailureListener { continuation.resumeWithException(it) } }
