package com.brandcrafts.erp.data.datasource.deliverychallan

import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.core.result.DeliveryChallanError
import com.brandcrafts.erp.core.result.DeliveryChallanFailure
import com.brandcrafts.erp.data.repository.DeliveryChallanActivityFactory
import com.brandcrafts.erp.data.repository.DeliveryChallanWritePolicy
import com.brandcrafts.erp.domain.model.DeliveryChallanDispatchRequest
import com.brandcrafts.erp.domain.model.UserRole
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreDeliveryChallanDispatchRemoteDataSource constructor(
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager,
) : DeliveryChallanDispatchRemoteDataSource {
    override suspend fun dispatch(request: DeliveryChallanDispatchRequest) {
        val actor = (sessionManager.currentUser.value as? CurrentUserState.Authenticated)?.user?.takeIf { it.active && it.role == UserRole.ADMIN && it.uid.isNotBlank() } ?: throw DeliveryChallanFailure(DeliveryChallanError.PermissionDenied)
        if (request.challanId.isBlank()) throw DeliveryChallanFailure(DeliveryChallanError.DeliveryChallanNotFound)
        val parent = firestore.collection(CHALLANS).document(request.challanId)
        val materialLines = parent.collection(ITEMS).get().awaitDispatchDc().documents
            .filter { !it.getString(MATERIAL).isNullOrBlank() }
        val duplicateStockOut = !firestore.collection(STOCK)
            .whereEqualTo(REFERENCE_TYPE, DELIVERY_CHALLAN)
            .whereEqualTo(REFERENCE_ID, request.challanId)
            .get()
            .awaitDispatchDc()
            .isEmpty
        DeliveryChallanWritePolicy.validate(
            DeliveryChallanWritePolicy.dispatchWriteCount(materialLines.size),
        )
        try {
            firestore.runTransaction { tx ->
                val current = tx.get(parent)
                if (!current.exists()) throw DeliveryChallanFailure(DeliveryChallanError.DeliveryChallanNotFound)
                if (current.getString(STATUS) != DRAFT) throw DeliveryChallanFailure(DeliveryChallanError.DispatchNotEligible)
                val number = current.getString(NUMBER)?.takeIf(String::isNotBlank) ?: throw DeliveryChallanFailure(DeliveryChallanError.InvalidLine)
                if (duplicateStockOut) throw DeliveryChallanFailure(DeliveryChallanError.DuplicateDispatchStockOut)
                val requestedMaterials = materialLines
                    .groupBy { line ->
                        line.getString(MATERIAL)
                            ?: throw DeliveryChallanFailure(DeliveryChallanError.InvalidLine)
                    }
                    .map { (materialId, matchingLines) ->
                        val units = matchingLines.map { line ->
                            line.getString(UNIT)?.takeIf(String::isNotBlank)
                                ?: throw DeliveryChallanFailure(DeliveryChallanError.InvalidUnit)
                        }.toSet()
                        if (units.size != 1) throw DeliveryChallanFailure(DeliveryChallanError.InvalidUnit)
                        MaterialRequest(
                            materialId = materialId,
                            quantity = matchingLines.fold(BigDecimal.ZERO) { total, line ->
                                total + decimal(line.getString(QUANTITY))
                            },
                            unit = units.single(),
                        )
                    }
                val prepared = requestedMaterials.map { requested ->
                    val material = firestore.collection(MATERIALS).document(requested.materialId)
                    val snapshot = tx.get(material)
                    if (!snapshot.exists() || snapshot.getBoolean("active") != true) throw DeliveryChallanFailure(DeliveryChallanError.InvalidLine)
                    val available = decimal(snapshot.get("availableQuantity")?.toString())
                    if (available < requested.quantity) throw DeliveryChallanFailure(DeliveryChallanError.InsufficientStock)
                    PreparedMaterial(
                        reference = material,
                        materialId = requested.materialId,
                        quantity = requested.quantity,
                        unit = requested.unit,
                        remaining = available.subtract(requested.quantity),
                    )
                }
                prepared.forEach { item ->
                    tx.update(item.reference, mapOf("availableQuantity" to item.remaining.toPlainString(), "updatedAt" to FieldValue.serverTimestamp(), "updatedBy" to actor.uid))
                    val stockId = firestore.collection(STOCK).document().id
                    tx.set(firestore.collection(STOCK).document(stockId), mapOf("id" to stockId, "materialId" to item.materialId, "transactionType" to "STOCK_OUT", "quantity" to item.quantity.toPlainString(), "unit" to item.unit, "referenceId" to request.challanId, "referenceType" to DELIVERY_CHALLAN, "remarks" to "", "performedBy" to actor.uid, "createdAt" to FieldValue.serverTimestamp()))
                }
                tx.update(parent, mapOf("status" to DISPATCHED, "dispatchedAt" to FieldValue.serverTimestamp(), "dispatchedBy" to actor.uid, "updatedAt" to FieldValue.serverTimestamp(), "updatedBy" to actor.uid))
                val activityId = firestore.collection(ACTIVITY).document().id; val activity = DeliveryChallanActivityFactory.dispatched(activityId, parent.id, number, actor.uid, actor.name)
                tx.set(firestore.collection(ACTIVITY).document(activityId), mapOf("id" to activityId, "module" to "DELIVERY_CHALLAN", "action" to activity.action, "referenceId" to parent.id, "referenceType" to DELIVERY_CHALLAN, "description" to number, "performedBy" to actor.uid, "performedByName" to actor.name, "createdAt" to FieldValue.serverTimestamp()))
            }.awaitDispatchDc()
        } catch (exception: CancellationException) { throw exception } catch (failure: DeliveryChallanFailure) { throw failure } catch (_: Throwable) { throw DeliveryChallanFailure(DeliveryChallanError.Unknown) }
    }
    private fun decimal(value: String?): BigDecimal = try { BigDecimal(value ?: throw DeliveryChallanFailure(DeliveryChallanError.InvalidQuantity)).also { if (it <= BigDecimal.ZERO) throw DeliveryChallanFailure(DeliveryChallanError.InvalidQuantity) } } catch (_: NumberFormatException) { throw DeliveryChallanFailure(DeliveryChallanError.InvalidQuantity) }
    private data class MaterialRequest(val materialId: String, val quantity: BigDecimal, val unit: String)
    private data class PreparedMaterial(val reference: com.google.firebase.firestore.DocumentReference, val materialId: String, val quantity: BigDecimal, val unit: String, val remaining: BigDecimal)
    private companion object { const val CHALLANS="delivery_challans"; const val ITEMS="items"; const val MATERIALS="materials"; const val STOCK="stock_transactions"; const val ACTIVITY="activity_logs"; const val STATUS="status"; const val DRAFT="DRAFT"; const val DISPATCHED="DISPATCHED"; const val NUMBER="dcNumber"; const val MATERIAL="materialId"; const val QUANTITY="quantity"; const val UNIT="unit"; const val REFERENCE_ID="referenceId"; const val REFERENCE_TYPE="referenceType"; const val DELIVERY_CHALLAN="DELIVERY_CHALLAN" }
}
private suspend fun <T> Task<T>.awaitDispatchDc(): T = suspendCancellableCoroutine { continuation -> addOnSuccessListener { continuation.resume(it) }.addOnFailureListener { continuation.resumeWithException(it) } }
