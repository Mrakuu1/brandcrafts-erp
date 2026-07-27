package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.core.result.DeliveryChallanError
import com.brandcrafts.erp.core.result.DeliveryChallanFailure

object DeliveryChallanWritePolicy {
    const val FIRESTORE_WRITE_LIMIT = 500
    const val SAFETY_MARGIN_WRITES = 25
    const val MAX_SAFE_WRITES = FIRESTORE_WRITE_LIMIT - SAFETY_MARGIN_WRITES
    private const val CREATE_FIXED_WRITES = 3
    private const val UPDATE_FIXED_WRITES = 2
    private const val DISPATCH_FIXED_WRITES = 2
    private const val CANCELLATION_FIXED_WRITES = 2

    fun createWriteCount(itemCount: Int): Int = itemCount + CREATE_FIXED_WRITES
    fun updateWriteCount(submittedItemCount: Int, staleDeleteCount: Int): Int = submittedItemCount + staleDeleteCount + UPDATE_FIXED_WRITES
    fun dispatchWriteCount(stockUpdateCount: Int): Int = stockUpdateCount + DISPATCH_FIXED_WRITES
    fun cancellationWriteCount(): Int = CANCELLATION_FIXED_WRITES
    fun validate(requestedWrites: Int) {
        if (requestedWrites < 0 || requestedWrites > MAX_SAFE_WRITES) {
            throw DeliveryChallanFailure(DeliveryChallanError.FirestoreOperationLimitExceeded(requestedWrites, MAX_SAFE_WRITES))
        }
    }
}
