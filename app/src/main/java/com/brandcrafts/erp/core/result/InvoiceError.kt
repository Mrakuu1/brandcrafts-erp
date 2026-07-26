package com.brandcrafts.erp.core.result

sealed interface InvoiceError {
    data object Unauthenticated : InvoiceError
    data object UserProfileMissing : InvoiceError
    data object InactiveUser : InvoiceError
    data object AdminAccessRequired : InvoiceError
    data object CustomerRequired : InvoiceError
    data object CustomerNotFound : InvoiceError
    data object ContactIsNotCustomer : InvoiceError
    data object CustomerInactive : InvoiceError
    data object InvoiceNotFound : InvoiceError
    data object WrongDocumentType : InvoiceError
    data object InvalidStoredStatus : InvoiceError
    data object InvalidStatusTransition : InvoiceError
    data object DraftOnlyUpdateRequired : InvoiceError
    data object InvoiceDateRequired : InvoiceError
    data object DueDateBeforeInvoiceDate : InvoiceError
    data object EmptyItemList : InvoiceError
    data object InvalidLine : InvoiceError
    data object InvalidQuantity : InvoiceError
    data object InvalidUnit : InvoiceError
    data object InvalidUnitPrice : InvoiceError
    data object MalformedStoredDate : InvoiceError
    data object MalformedStoredDecimal : InvoiceError
    data object InvalidPaidAmount : InvoiceError
    data object PaymentExceedsOutstandingAmount : InvoiceError
    data object PaymentNotAllowedForCurrentStatus : InvoiceError
    data object InvoiceHasRecordedPayments : InvoiceError
    data class FirestoreOperationLimitExceeded(val requestedWrites: Int, val maximumWrites: Int) : InvoiceError
    data object CompanyConfigurationMissing : InvoiceError
    data object PermissionDenied : InvoiceError
    data object FirestoreUnavailable : InvoiceError
    data object CounterValueMalformed : InvoiceError
    data object CounterValueOverflow : InvoiceError
    data object InvalidOperationCount : InvoiceError
    data object InvalidGeneratedInvoiceNumber : InvoiceError
    data object InvalidGeneratedDocumentId : InvoiceError
    data object RepositoryUnavailable : InvoiceError
    data object Unknown : InvoiceError
}

class InvoiceFailure(val error: InvoiceError) : RuntimeException()
