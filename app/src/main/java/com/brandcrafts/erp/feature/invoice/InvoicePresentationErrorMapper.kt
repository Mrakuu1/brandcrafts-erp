package com.brandcrafts.erp.feature.invoice

import androidx.annotation.StringRes
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.result.InvoiceError
import com.brandcrafts.erp.core.result.InvoiceFailure

internal data class InvoicePresentationMessage(
    @StringRes val messageRes: Int,
    val unauthorized: Boolean = false,
)

internal object InvoicePresentationErrorMapper {
    fun map(throwable: Throwable): InvoicePresentationMessage =
        (throwable as? InvoiceFailure)?.error?.toPresentationMessage()
            ?: InvoicePresentationMessage(R.string.invoice_error_generic)
}

internal fun InvoiceError.toPresentationMessage(): InvoicePresentationMessage = when (this) {
    InvoiceError.Unauthenticated,
    InvoiceError.AdminAccessRequired,
    InvoiceError.PermissionDenied -> InvoicePresentationMessage(
        R.string.invoice_error_unauthorized,
        unauthorized = true,
    )
    InvoiceError.UserProfileMissing -> InvoicePresentationMessage(
        R.string.invoice_error_user_profile_missing,
        unauthorized = true,
    )
    InvoiceError.InactiveUser -> InvoicePresentationMessage(
        R.string.invoice_error_user_inactive,
        unauthorized = true,
    )
    InvoiceError.CustomerRequired -> InvoicePresentationMessage(R.string.invoice_error_customer_required)
    InvoiceError.CustomerNotFound -> InvoicePresentationMessage(R.string.invoice_error_customer_not_found)
    InvoiceError.ContactIsNotCustomer -> InvoicePresentationMessage(R.string.invoice_error_not_customer)
    InvoiceError.CustomerInactive -> InvoicePresentationMessage(R.string.invoice_error_customer_inactive)
    InvoiceError.InvoiceNotFound -> InvoicePresentationMessage(R.string.invoice_error_not_found)
    InvoiceError.WrongDocumentType,
    InvoiceError.InvalidStoredStatus,
    InvoiceError.MalformedStoredDate,
    InvoiceError.MalformedStoredDecimal -> InvoicePresentationMessage(R.string.invoice_error_invalid_data)
    InvoiceError.InvalidStatusTransition -> InvoicePresentationMessage(R.string.invoice_error_invalid_status_transition)
    InvoiceError.DraftOnlyUpdateRequired -> InvoicePresentationMessage(R.string.invoice_error_draft_only_edit)
    InvoiceError.InvoiceDateRequired -> InvoicePresentationMessage(R.string.invoice_error_date_required)
    InvoiceError.DueDateBeforeInvoiceDate -> InvoicePresentationMessage(R.string.invoice_error_due_date_before_invoice_date)
    InvoiceError.EmptyItemList -> InvoicePresentationMessage(R.string.invoice_error_items_required)
    InvoiceError.InvalidLine -> InvoicePresentationMessage(R.string.invoice_error_invalid_line)
    InvoiceError.InvalidQuantity -> InvoicePresentationMessage(R.string.invoice_error_invalid_quantity)
    InvoiceError.InvalidUnit -> InvoicePresentationMessage(R.string.invoice_error_invalid_unit)
    InvoiceError.InvalidUnitPrice -> InvoicePresentationMessage(R.string.invoice_error_invalid_unit_price)
    InvoiceError.InvalidPaidAmount -> InvoicePresentationMessage(R.string.invoice_error_invalid_paid_amount)
    InvoiceError.PaymentExceedsOutstandingAmount -> InvoicePresentationMessage(R.string.invoice_error_payment_exceeds_outstanding)
    InvoiceError.PaymentNotAllowedForCurrentStatus -> InvoicePresentationMessage(R.string.invoice_error_payment_not_allowed)
    InvoiceError.InvoiceHasRecordedPayments -> InvoicePresentationMessage(R.string.invoice_error_cancellation_has_payments)
    is InvoiceError.FirestoreOperationLimitExceeded -> InvoicePresentationMessage(R.string.invoice_error_operation_limit)
    InvoiceError.CompanyConfigurationMissing -> InvoicePresentationMessage(R.string.invoice_pdf_company_configuration_error)
    InvoiceError.FirestoreUnavailable -> InvoicePresentationMessage(R.string.invoice_error_retry)
    InvoiceError.CounterValueMalformed,
    InvoiceError.CounterValueOverflow,
    InvoiceError.InvalidOperationCount,
    InvoiceError.InvalidGeneratedInvoiceNumber,
    InvoiceError.InvalidGeneratedDocumentId,
    InvoiceError.RepositoryUnavailable,
    InvoiceError.Unknown -> InvoicePresentationMessage(R.string.invoice_error_generic)
}
