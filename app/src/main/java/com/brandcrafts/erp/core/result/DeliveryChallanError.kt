package com.brandcrafts.erp.core.result

sealed interface DeliveryChallanError {
    data object Unauthenticated : DeliveryChallanError
    data object UserProfileMissing : DeliveryChallanError
    data object InactiveUser : DeliveryChallanError
    data object DeliveryChallanNotFound : DeliveryChallanError
    data object CustomerRequired : DeliveryChallanError
    data object CustomerNotFound : DeliveryChallanError
    data object CustomerInactive : DeliveryChallanError
    data object DeliveryAddressRequired : DeliveryChallanError
    data object DeliveryDateRequired : DeliveryChallanError
    data object InvalidInvoiceSource : DeliveryChallanError
    data object InvoiceAlreadyHasActiveDeliveryChallan : DeliveryChallanError
    data object DraftOnlyUpdateRequired : DeliveryChallanError
    data object InvalidStatusTransition : DeliveryChallanError
    data object EmptyItemList : DeliveryChallanError
    data object InvalidLine : DeliveryChallanError
    data object InvalidLineId : DeliveryChallanError
    data object InvalidQuantity : DeliveryChallanError
    data object InvalidUnit : DeliveryChallanError
    data object InvoiceQuantityExceeded : DeliveryChallanError
    data object DispatchNotEligible : DeliveryChallanError
    data object DraftCancellationRequired : DeliveryChallanError
    data object InsufficientStock : DeliveryChallanError
    data object DuplicateDispatchStockOut : DeliveryChallanError
    data object InvalidStoredStatus : DeliveryChallanError
    data object MalformedStoredDate : DeliveryChallanError
    data object PermissionDenied : DeliveryChallanError
    data object FirestoreUnavailable : DeliveryChallanError
    data object CounterValueMalformed : DeliveryChallanError
    data object CounterValueOverflow : DeliveryChallanError
    data class FirestoreOperationLimitExceeded(val requestedWrites: Int, val maximumWrites: Int) : DeliveryChallanError
    data object Unknown : DeliveryChallanError
}

class DeliveryChallanFailure(val error: DeliveryChallanError) : RuntimeException()
