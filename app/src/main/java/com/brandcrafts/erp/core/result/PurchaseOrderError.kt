package com.brandcrafts.erp.core.result

sealed interface PurchaseOrderError {
    data object Unauthenticated : PurchaseOrderError
    data object UserProfileMissing : PurchaseOrderError
    data object InactiveUser : PurchaseOrderError
    data object AdminAccessRequired : PurchaseOrderError
    data object PurchaseOrderNotFound : PurchaseOrderError
    data object WrongDocumentType : PurchaseOrderError
    data object InvalidStoredStatus : PurchaseOrderError
    data object InvalidStatusTransition : PurchaseOrderError
    data object DraftOnlyUpdateRequired : PurchaseOrderError
    data object SupplierNotFound : PurchaseOrderError
    data object SupplierInactive : PurchaseOrderError
    data object ContactIsNotSupplier : PurchaseOrderError
    data object PurchaseOrderDateMissing : PurchaseOrderError
    data object EmptyItemList : PurchaseOrderError
    data object InvalidQuantity : PurchaseOrderError
    data object NegativeUnitPrice : PurchaseOrderError
    data object InvalidDecimalInput : PurchaseOrderError
    data object MalformedStoredDecimal : PurchaseOrderError
    data object MalformedStoredDate : PurchaseOrderError
    data object CounterValueMalformed : PurchaseOrderError
    data object CounterValueOverflow : PurchaseOrderError
    data object CounterTransactionFailed : PurchaseOrderError
    data object PurchaseOrderWriteFailed : PurchaseOrderError
    data object LineItemWriteFailed : PurchaseOrderError
    data object ActivityLogWriteFailed : PurchaseOrderError
    data class FirestoreOperationLimitExceeded(val requestedWrites: Int, val maximumWrites: Int) : PurchaseOrderError
    data object StockReferenceValidationUnavailable : PurchaseOrderError
    data object PermissionDenied : PurchaseOrderError
    data object FirestoreUnavailable : PurchaseOrderError
    data object TransactionAborted : PurchaseOrderError
    data object Unknown : PurchaseOrderError
}

class PurchaseOrderFailure(val error: PurchaseOrderError) : RuntimeException()
