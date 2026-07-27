package com.brandcrafts.erp.data.datasource.deliverychallan

import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.core.result.ContactResult
import com.brandcrafts.erp.core.result.DeliveryChallanError
import com.brandcrafts.erp.core.result.DeliveryChallanFailure
import com.brandcrafts.erp.data.repository.DeliveryChallanActivityFactory
import com.brandcrafts.erp.data.repository.DeliveryChallanCounter
import com.brandcrafts.erp.data.repository.DeliveryChallanWritePolicy
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.model.DeliveryChallanCreateRequest
import com.brandcrafts.erp.domain.model.DeliveryChallanSourceType
import com.brandcrafts.erp.domain.model.DeliveryChallanStatus
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.domain.repository.ContactRepository
import com.brandcrafts.erp.domain.usecase.deliverychallan.DeliveryChallanValidator
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.Timestamp
import java.util.Date
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreDeliveryChallanCreateRemoteDataSource constructor(
    private val firestore: FirebaseFirestore,
    private val contacts: ContactRepository,
    private val sessionManager: SessionManager,
    private val validator: DeliveryChallanValidator,
) : DeliveryChallanCreateRemoteDataSource {
    override suspend fun createIndependent(request: DeliveryChallanCreateRequest): String {
        val actor = activeActor()
        if (request.sourceType != DeliveryChallanSourceType.INDEPENDENT || request.sourceInvoiceId != null || request.sourceInvoiceNumber != null) {
            throw DeliveryChallanFailure(DeliveryChallanError.InvalidInvoiceSource)
        }
        validator.validateDraft(request.customerId, request.deliveryAddress, request.dateMillis, request.sourceType, request.sourceInvoiceId, request.sourceInvoiceNumber, request.lines).getOrElse { throw it }
        validateCustomer(request.customerId)
        DeliveryChallanWritePolicy.validate(DeliveryChallanWritePolicy.createWriteCount(request.lines.size))
        val parent = firestore.collection(COLLECTION).document()
        val itemIds = request.lines.map { line -> line.id.takeIf(String::isNotBlank) ?: parent.collection(ITEMS).document().id }
        if (itemIds.toSet().size != itemIds.size || itemIds.any(String::isBlank)) throw DeliveryChallanFailure(DeliveryChallanError.InvalidLineId)
        val activityId = firestore.collection(ACTIVITY).document().id
        if (activityId.isBlank()) throw DeliveryChallanFailure(DeliveryChallanError.Unknown)
        try {
            firestore.runTransaction { transaction ->
                val counter = firestore.document(DeliveryChallanCounter.COUNTER_DOCUMENT_PATH)
                val current = DeliveryChallanCounter.currentValue(transaction.get(counter).get("value"))
                val next = DeliveryChallanCounter.nextValue(current)
                val number = DeliveryChallanCounter.format(next)
                if (transaction.get(parent).exists()) throw DeliveryChallanFailure(DeliveryChallanError.Unknown)
                transaction.set(counter, mapOf("value" to next))
                transaction.set(parent, parentMap(parent.id, number, request, actor.id))
                request.lines.forEachIndexed { index, line ->
                    transaction.set(parent.collection(ITEMS).document(itemIds[index]), mapOf("itemId" to itemIds[index], "materialId" to line.materialId, "description" to line.description, "quantity" to line.quantity.toPlainString(), "unit" to line.unit, "sortOrder" to index))
                }
                val activity = DeliveryChallanActivityFactory.created(activityId, parent.id, number, actor.id, actor.name)
                transaction.set(firestore.collection(ACTIVITY).document(activity.activityId), mapOf("id" to activity.activityId, "module" to "DELIVERY_CHALLAN", "action" to activity.action, "referenceId" to activity.challanId, "referenceType" to "DELIVERY_CHALLAN", "description" to activity.challanNumber, "performedBy" to activity.actorId, "performedByName" to activity.actorDisplayName, "createdAt" to FieldValue.serverTimestamp()))
            }.awaitDc()
            return parent.id
        } catch (exception: CancellationException) { throw exception
        } catch (failure: DeliveryChallanFailure) { throw failure
        } catch (exception: FirebaseFirestoreException) { throw DeliveryChallanFailure(if (exception.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) DeliveryChallanError.PermissionDenied else if (exception.code == FirebaseFirestoreException.Code.UNAVAILABLE) DeliveryChallanError.FirestoreUnavailable else DeliveryChallanError.Unknown)
        } catch (_: Throwable) { throw DeliveryChallanFailure(DeliveryChallanError.Unknown) }
    }
    private fun parentMap(id: String, number: String, request: DeliveryChallanCreateRequest, actorId: String) = mapOf("id" to id, "dcNumber" to number, "customerId" to request.customerId, "deliveryAddress" to request.deliveryAddress.trim(), "date" to Timestamp(Date(request.dateMillis)), "sourceType" to request.sourceType.name, "sourceInvoiceId" to null, "sourceInvoiceNumber" to null, "vehicleNumber" to request.vehicleNumber.trim(), "driverName" to request.driverName.trim(), "notes" to request.notes.trim(), "status" to DeliveryChallanStatus.DRAFT.name, "createdBy" to actorId, "updatedBy" to actorId, "createdAt" to FieldValue.serverTimestamp(), "updatedAt" to FieldValue.serverTimestamp())
    private fun activeActor(): Actor {
        val user = (sessionManager.currentUser.value as? CurrentUserState.Authenticated)?.user
            ?: throw DeliveryChallanFailure(DeliveryChallanError.Unauthenticated)
        if (!user.active) throw DeliveryChallanFailure(DeliveryChallanError.InactiveUser)
        if (user.uid.isBlank()) throw DeliveryChallanFailure(DeliveryChallanError.UserProfileMissing)
        if (user.role != UserRole.ADMIN) throw DeliveryChallanFailure(DeliveryChallanError.PermissionDenied)
        return Actor(user.uid, user.name)
    }
    private suspend fun validateCustomer(id: String) { when (val result = contacts.getContact(id)) { is ContactResult.Success -> if (!result.data.active) throw DeliveryChallanFailure(DeliveryChallanError.CustomerInactive) else if (result.data.type != ContactType.CUSTOMER) throw DeliveryChallanFailure(DeliveryChallanError.CustomerNotFound); is ContactResult.Error -> throw DeliveryChallanFailure(DeliveryChallanError.CustomerNotFound) } }
    private data class Actor(val id: String, val name: String)
    private companion object { const val COLLECTION="delivery_challans"; const val ITEMS="items"; const val ACTIVITY="activity_logs" }
}

private suspend fun <T> Task<T>.awaitDc(): T = suspendCancellableCoroutine { continuation -> addOnSuccessListener { continuation.resume(it) }.addOnFailureListener { continuation.resumeWithException(it) } }
