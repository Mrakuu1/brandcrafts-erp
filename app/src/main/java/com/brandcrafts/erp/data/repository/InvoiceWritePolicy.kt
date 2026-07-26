package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.core.result.InvoiceError
import com.brandcrafts.erp.core.result.InvoiceFailure

internal object InvoiceWritePolicy {
    const val FIRESTORE_WRITE_LIMIT = 500
    const val SAFETY_MARGIN_WRITES = 25
    const val MAX_SAFE_WRITES = FIRESTORE_WRITE_LIMIT - SAFETY_MARGIN_WRITES
    const val CREATE_FIXED_WRITES = 3
    const val MAX_CREATE_LINE_COUNT = MAX_SAFE_WRITES - CREATE_FIXED_WRITES
    fun createWriteCount(itemCount: Int): Int = checked(itemCount, 0) + CREATE_FIXED_WRITES
    fun updateWriteCount(submittedItemCount: Int, staleDeleteCount: Int): Int = checked(submittedItemCount, staleDeleteCount) + 2
    fun issueWriteCount() = 2
    fun cancellationWriteCount() = 2
    fun paymentWriteCount() = 2
    fun validateCreate(itemCount: Int) = validate(createWriteCount(itemCount))
    fun validateUpdate(submittedItemCount: Int, staleDeleteCount: Int) = validate(updateWriteCount(submittedItemCount, staleDeleteCount))
    fun validateIssue() = validate(issueWriteCount())
    fun validateCancellation() = validate(cancellationWriteCount())
    fun validatePayment() = validate(paymentWriteCount())
    private fun validate(count: Int) { if (count > MAX_SAFE_WRITES) throw InvoiceFailure(InvoiceError.FirestoreOperationLimitExceeded(count, MAX_SAFE_WRITES)) }
    private fun checked(first: Int, second: Int): Int { if (first < 0 || second < 0) throw InvoiceFailure(InvoiceError.InvalidOperationCount); return try { Math.addExact(first, second) } catch (_: ArithmeticException) { throw InvoiceFailure(InvoiceError.FirestoreOperationLimitExceeded(Int.MAX_VALUE, MAX_SAFE_WRITES)) } }
}
