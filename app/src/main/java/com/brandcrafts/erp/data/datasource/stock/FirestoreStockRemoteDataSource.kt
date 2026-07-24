package com.brandcrafts.erp.data.datasource.stock

import com.brandcrafts.erp.data.model.FirestoreStockIn
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class FirestoreStockRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) : StockRemoteDataSource {
    override suspend fun stockIn(input: FirestoreStockIn) {
        val material = firestore.collection(MATERIALS).document(input.materialId)
        val transactionRecord = firestore.collection(STOCK_TRANSACTIONS).document()
        val activityRecord = firestore.collection(ACTIVITY_LOGS).document()
        firestore.runTransaction { transaction ->
            val materialSnapshot = transaction.get(material)
            if (!materialSnapshot.exists()) throw MaterialNotFoundException
            if (materialSnapshot.getBoolean("active") != true) throw MaterialInactiveException
            val currentQuantity = materialSnapshot.getDouble("availableQuantity") ?: 0.0
            transaction.update(material, mapOf(
                "availableQuantity" to currentQuantity + input.quantity,
                "updatedAt" to FieldValue.serverTimestamp(),
                "updatedBy" to input.performedBy,
            ))
            transaction.set(transactionRecord, mapOf(
                "id" to transactionRecord.id,
                "materialId" to input.materialId,
                "transactionType" to "STOCK_IN",
                "quantity" to input.quantity,
                "unit" to input.unit,
                "referenceId" to input.referenceId,
                "referenceType" to "MANUAL",
                "supplierId" to "",
                "remarks" to input.remarks,
                "performedBy" to input.performedBy,
                "createdAt" to FieldValue.serverTimestamp(),
            ))
            transaction.set(activityRecord, mapOf(
                "id" to activityRecord.id,
                "module" to "INVENTORY",
                "action" to "STOCK_IN",
                "referenceId" to transactionRecord.id,
                "referenceType" to "STOCK_TRANSACTION",
                "description" to "Stock in recorded",
                "performedBy" to input.performedBy,
                "performedByName" to input.performedByName,
                "createdAt" to FieldValue.serverTimestamp(),
            ))
        }.await()
    }

    private companion object { const val MATERIALS = "materials"; const val STOCK_TRANSACTIONS = "stock_transactions"; const val ACTIVITY_LOGS = "activity_logs" }
}

data object MaterialNotFoundException : IllegalStateException()
data object MaterialInactiveException : IllegalStateException()

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { continuation.resume(it) }.addOnFailureListener { continuation.resumeWithException(it) }
}
