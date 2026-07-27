package com.brandcrafts.erp.data.datasource.deliverychallan

import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.core.result.DeliveryChallanError
import com.brandcrafts.erp.core.result.DeliveryChallanFailure
import com.brandcrafts.erp.data.repository.DeliveryChallanActivityFactory
import com.brandcrafts.erp.data.repository.DeliveryChallanWritePolicy
import com.brandcrafts.erp.domain.model.UserRole
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreDeliveryChallanCancellationRemoteDataSource constructor(
    private val firestore: FirebaseFirestore,
    private val sessionManager: SessionManager,
) : DeliveryChallanCancellationRemoteDataSource {
    override suspend fun cancelDraft(challanId: String) {
        val actor = (sessionManager.currentUser.value as? CurrentUserState.Authenticated)?.user
            ?.takeIf { it.active && it.role == UserRole.ADMIN && it.uid.isNotBlank() }
            ?: throw DeliveryChallanFailure(DeliveryChallanError.PermissionDenied)
        if (challanId.isBlank()) throw DeliveryChallanFailure(DeliveryChallanError.DeliveryChallanNotFound)
        DeliveryChallanWritePolicy.validate(DeliveryChallanWritePolicy.cancellationWriteCount())
        val parent = firestore.collection(CHALLANS).document(challanId)
        try {
            firestore.runTransaction { transaction ->
                val current = transaction.get(parent)
                if (!current.exists()) throw DeliveryChallanFailure(DeliveryChallanError.DeliveryChallanNotFound)
                if (current.getString(STATUS) != DRAFT) throw DeliveryChallanFailure(DeliveryChallanError.DraftCancellationRequired)
                val number = current.getString(NUMBER)?.takeIf(String::isNotBlank) ?: throw DeliveryChallanFailure(DeliveryChallanError.InvalidLine)
                transaction.update(parent, mapOf("status" to CANCELLED, "cancelledAt" to FieldValue.serverTimestamp(), "cancelledBy" to actor.uid, "updatedAt" to FieldValue.serverTimestamp(), "updatedBy" to actor.uid))
                val activityId = firestore.collection(ACTIVITY).document().id
                val activity = DeliveryChallanActivityFactory.cancelled(activityId, challanId, number, actor.uid, actor.name)
                transaction.set(firestore.collection(ACTIVITY).document(activityId), mapOf("id" to activity.activityId, "module" to "DELIVERY_CHALLAN", "action" to activity.action, "referenceId" to challanId, "referenceType" to "DELIVERY_CHALLAN", "description" to number, "performedBy" to actor.uid, "performedByName" to actor.name, "createdAt" to FieldValue.serverTimestamp()))
            }.awaitCancellationDc()
        } catch (exception: CancellationException) { throw exception } catch (failure: DeliveryChallanFailure) { throw failure } catch (_: Throwable) { throw DeliveryChallanFailure(DeliveryChallanError.Unknown) }
    }
    private companion object { const val CHALLANS="delivery_challans"; const val ACTIVITY="activity_logs"; const val STATUS="status"; const val DRAFT="DRAFT"; const val CANCELLED="CANCELLED"; const val NUMBER="dcNumber" }
}
private suspend fun <T> Task<T>.awaitCancellationDc(): T = suspendCancellableCoroutine { continuation -> addOnSuccessListener { continuation.resume(it) }.addOnFailureListener { continuation.resumeWithException(it) } }
