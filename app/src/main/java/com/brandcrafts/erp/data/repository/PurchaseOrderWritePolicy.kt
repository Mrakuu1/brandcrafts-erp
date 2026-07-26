package com.brandcrafts.erp.data.repository

internal object PurchaseOrderWritePolicy {
    private const val FIRESTORE_TRANSACTION_WRITE_LIMIT = 500
    private const val SAFETY_MARGIN_WRITES = 25
    const val MAX_SAFE_WRITES = FIRESTORE_TRANSACTION_WRITE_LIMIT - SAFETY_MARGIN_WRITES
    const val CREATE_FIXED_WRITES = 3
    fun createWriteCount(itemCount: Int) = CREATE_FIXED_WRITES + itemCount
    fun updateWriteCount(itemWriteCount: Int, staleDeleteCount: Int) = itemWriteCount + staleDeleteCount + 2
    fun isAllowed(writeCount: Int) = writeCount <= MAX_SAFE_WRITES
}
