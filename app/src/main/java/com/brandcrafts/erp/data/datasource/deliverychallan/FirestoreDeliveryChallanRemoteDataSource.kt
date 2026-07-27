package com.brandcrafts.erp.data.datasource.deliverychallan

import com.brandcrafts.erp.core.result.DeliveryChallanError
import com.brandcrafts.erp.core.result.DeliveryChallanFailure
import com.brandcrafts.erp.data.model.deliverychallan.DeliveryChallanDto
import com.brandcrafts.erp.data.model.deliverychallan.DeliveryChallanLineDto
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreDeliveryChallanRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) : DeliveryChallanRemoteDataSource {
    override fun observeParents() = callbackFlow {
        val registration = firestore.collection(COLLECTION).orderBy(DATE, Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) close(mapReadFailure(error))
                else if (snapshot != null) {
                    trySend(snapshot.documents.map(::parentDto))
                }
            }
        awaitClose(registration::remove)
    }

    override suspend fun getParent(challanId: String): DeliveryChallanDto? {
        validateId(challanId)
        val document = firestore.collection(COLLECTION).document(challanId).get().awaitDeliveryChallan()
        return document.takeIf(DocumentSnapshot::exists)?.let(::parentDto)
    }

    override suspend fun getLines(challanId: String): List<DeliveryChallanLineDto> {
        validateId(challanId)
        return firestore.collection(COLLECTION).document(challanId).collection(ITEMS).orderBy(SORT_ORDER)
            .get().awaitDeliveryChallan().documents.map(::lineDto)
    }

    private fun validateId(value: String) { if (value.isBlank()) throw DeliveryChallanFailure(DeliveryChallanError.DeliveryChallanNotFound) }
    private fun parentDto(document: DocumentSnapshot) = DeliveryChallanDto(document.id, document.getString(NUMBER), document.getString(CUSTOMER), document.getString(ADDRESS), document.get(DATE), document.getString(SOURCE_TYPE), document.getString(SOURCE_INVOICE_ID), document.getString(SOURCE_INVOICE_NUMBER), document.getString(VEHICLE), document.getString(DRIVER), document.getString(NOTES), document.getString(STATUS), document.get(CREATED_AT), document.get(UPDATED_AT), document.getString(CREATED_BY), document.getString(UPDATED_BY), document.get(DISPATCHED_AT), document.getString(DISPATCHED_BY), document.get(CANCELLED_AT), document.getString(CANCELLED_BY))
    private fun lineDto(document: DocumentSnapshot) = DeliveryChallanLineDto(document.id, document.getString(MATERIAL), document.getString(DESCRIPTION), document.getString(QUANTITY), document.getString(UNIT), document.getLong(SORT_ORDER)?.toInt())
    private fun mapReadFailure(error: FirebaseFirestoreException) = DeliveryChallanFailure(when (error.code) { FirebaseFirestoreException.Code.PERMISSION_DENIED -> DeliveryChallanError.PermissionDenied; FirebaseFirestoreException.Code.UNAVAILABLE -> DeliveryChallanError.FirestoreUnavailable; else -> DeliveryChallanError.Unknown })
    private companion object { const val COLLECTION="delivery_challans"; const val ITEMS="items"; const val NUMBER="dcNumber"; const val CUSTOMER="customerId"; const val ADDRESS="deliveryAddress"; const val DATE="date"; const val SOURCE_TYPE="sourceType"; const val SOURCE_INVOICE_ID="sourceInvoiceId"; const val SOURCE_INVOICE_NUMBER="sourceInvoiceNumber"; const val VEHICLE="vehicleNumber"; const val DRIVER="driverName"; const val NOTES="notes"; const val STATUS="status"; const val CREATED_AT="createdAt"; const val CREATED_BY="createdBy"; const val UPDATED_AT="updatedAt"; const val UPDATED_BY="updatedBy"; const val DISPATCHED_AT="dispatchedAt"; const val DISPATCHED_BY="dispatchedBy"; const val CANCELLED_AT="cancelledAt"; const val CANCELLED_BY="cancelledBy"; const val MATERIAL="materialId"; const val DESCRIPTION="description"; const val QUANTITY="quantity"; const val UNIT="unit"; const val SORT_ORDER="sortOrder" }
}

private suspend fun <T> Task<T>.awaitDeliveryChallan(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
    addOnFailureListener { continuation.resumeWithException(it) }
}
