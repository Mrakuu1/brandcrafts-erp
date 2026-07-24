package com.brandcrafts.erp.data.datasource.stock

import com.brandcrafts.erp.domain.model.AuthenticatedUser
import com.brandcrafts.erp.domain.model.StockOutInput
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

interface StockOutRemoteDataSource { suspend fun stockOut(input: StockOutInput, user: AuthenticatedUser) }

class FirestoreStockOutRemoteDataSource @Inject constructor(private val firestore: FirebaseFirestore) : StockOutRemoteDataSource {
    override suspend fun stockOut(input: StockOutInput, user: AuthenticatedUser) {
        val material = firestore.collection("materials").document(input.materialId)
        val transactionRecord = firestore.collection("stock_transactions").document()
        val activityRecord = firestore.collection("activity_logs").document()
        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(material)
            if (!snapshot.exists()) throw StockOutMaterialNotFoundException
            if (snapshot.getBoolean("active") != true) throw StockOutMaterialInactiveException
            val available = snapshot.getDouble("availableQuantity") ?: 0.0
            if (available < input.quantity) throw InsufficientStockException
            transaction.update(material, mapOf("availableQuantity" to available - input.quantity, "updatedAt" to FieldValue.serverTimestamp(), "updatedBy" to user.uid))
            transaction.set(transactionRecord, mapOf("id" to transactionRecord.id, "materialId" to input.materialId, "transactionType" to "STOCK_OUT", "quantity" to input.quantity, "unit" to (snapshot.getString("unit") ?: ""), "referenceId" to input.referenceId.trim(), "referenceType" to "MANUAL", "supplierId" to "", "remarks" to input.remarks.trim(), "performedBy" to user.uid, "createdAt" to FieldValue.serverTimestamp()))
            transaction.set(activityRecord, mapOf("id" to activityRecord.id, "module" to "INVENTORY", "action" to "STOCK_OUT", "referenceId" to transactionRecord.id, "referenceType" to "STOCK_TRANSACTION", "description" to "Stock out recorded", "performedBy" to user.uid, "performedByName" to user.name, "createdAt" to FieldValue.serverTimestamp()))
        }.await()
    }
}
data object StockOutMaterialNotFoundException : IllegalStateException()
data object StockOutMaterialInactiveException : IllegalStateException()
data object InsufficientStockException : IllegalStateException()
private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation -> addOnSuccessListener { continuation.resume(it) }.addOnFailureListener { continuation.resumeWithException(it) } }
