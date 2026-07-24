package com.brandcrafts.erp.data.datasource.inventory

import com.brandcrafts.erp.data.mapper.toCreateMap
import com.brandcrafts.erp.data.mapper.toFirestoreInventoryItem
import com.brandcrafts.erp.data.mapper.toUpdateMap
import com.brandcrafts.erp.data.model.FirestoreInventoryItem
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreInventoryRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) : InventoryRemoteDataSource {

    override fun observeItems(): Flow<List<FirestoreInventoryItem>> = callbackFlow {
        val registration = firestore.collection(MATERIALS_COLLECTION)
            .addSnapshotListener { snapshot, exception ->
                if (exception != null) {
                    close(exception)
                } else {
                    trySend(snapshot?.documents.orEmpty().map { it.toFirestoreInventoryItem() })
                }
            }

        awaitClose(registration::remove)
    }

    override suspend fun getItem(id: String): FirestoreInventoryItem? = firestore
        .collection(MATERIALS_COLLECTION)
        .document(id)
        .get()
        .await()
        .takeIf { it.exists() }
        ?.toFirestoreInventoryItem()

    override suspend fun createItem(item: FirestoreInventoryItem) {
        val document = firestore.collection(MATERIALS_COLLECTION).document()
        document.set(item.toCreateMap(document.id)).await()
    }

    override suspend fun updateItem(item: FirestoreInventoryItem) {
        firestore.collection(MATERIALS_COLLECTION)
            .document(item.id)
            .update(item.toUpdateMap())
            .await()
    }

    override suspend fun isSkuInUse(sku: String, excludingId: String?): Boolean = firestore
        .collection(MATERIALS_COLLECTION)
        .whereEqualTo("sku", sku)
        .get()
        .await()
        .documents
        .any { document -> document.id != excludingId }

    private companion object {
        const val MATERIALS_COLLECTION = "materials"
    }
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }
        .addOnFailureListener { continuation.resumeWithException(it) }
}
