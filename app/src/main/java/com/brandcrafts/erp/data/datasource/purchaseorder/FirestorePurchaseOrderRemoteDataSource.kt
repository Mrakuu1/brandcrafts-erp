package com.brandcrafts.erp.data.datasource.purchaseorder

import com.brandcrafts.erp.domain.model.PurchaseOrder
import com.brandcrafts.erp.domain.model.PurchaseOrderStatus
import com.brandcrafts.erp.core.result.PurchaseOrderError
import com.brandcrafts.erp.core.result.PurchaseOrderFailure
import com.brandcrafts.erp.data.mapper.toPurchaseOrderDateMillis
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.math.BigDecimal
import javax.inject.Inject
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow

class FirestorePurchaseOrderRemoteDataSource @Inject constructor(
    private val firestore: FirebaseFirestore,
) : PurchaseOrderRemoteDataSource {
    override fun observePurchaseOrders() = callbackFlow {
        val listener = firestore.collection(DOCUMENTS)
            .whereEqualTo("type", TYPE)
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) close(error)
                else if (snapshot != null) runCatching { snapshot.documents.map(::toPurchaseOrder) }
                    .onSuccess { trySend(it) }
                    .onFailure { close(it) }
            }
        awaitClose(listener::remove)
    }

    private fun toPurchaseOrder(document: com.google.firebase.firestore.DocumentSnapshot) = PurchaseOrder(
        id = document.id.takeIf { it.isNotBlank() } ?: throw PurchaseOrderFailure(PurchaseOrderError.PurchaseOrderNotFound),
        number = required(document, "documentNumber"),
        supplierId = required(document, "supplierId"),
        dateMillis = document.get("date").toPurchaseOrderDateMillis(required = true),
        expectedDeliveryDateMillis = document.get("expectedDeliveryDate").toPurchaseOrderDateMillis(required = false),
        supplierReferenceNumber = document.getString("supplierReferenceNumber").orEmpty(),
        remarks = document.getString("remarks").orEmpty(),
        status = parseStatus(document.getString("status")),
        total = BigDecimal(required(document, "total")),
        createdAtMillis = document.getTimestamp("createdAt")?.toDate()?.time,
        updatedAtMillis = document.getTimestamp("updatedAt")?.toDate()?.time,
        createdBy = required(document, "createdBy"),
        updatedBy = required(document, "updatedBy"),
        approvedAtMillis = document.getTimestamp("approvedAt")?.toDate()?.time,
        approvedBy = document.getString("approvedBy").orEmpty(),
        cancelledAtMillis = document.getTimestamp("cancelledAt")?.toDate()?.time,
        cancelledBy = document.getString("cancelledBy").orEmpty(),
    )

    private fun required(document: com.google.firebase.firestore.DocumentSnapshot, field: String): String =
        document.getString(field)?.takeIf { it.isNotBlank() } ?: throw PurchaseOrderFailure(PurchaseOrderError.PurchaseOrderNotFound)
    private fun parseStatus(value: String?): PurchaseOrderStatus =
        PurchaseOrderStatus.entries.firstOrNull { it.name == value } ?: throw PurchaseOrderFailure(PurchaseOrderError.InvalidStoredStatus)

    private companion object { const val DOCUMENTS = "documents"; const val TYPE = "PURCHASE_ORDER" }
}
