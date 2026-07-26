package com.brandcrafts.erp.feature.invoice

sealed interface InvoiceDetailsUiEvent {
    data object Retry : InvoiceDetailsUiEvent
    data object Back : InvoiceDetailsUiEvent
    data object EditClicked : InvoiceDetailsUiEvent
    data object IssueClicked : InvoiceDetailsUiEvent
    data object IssueConfirmed : InvoiceDetailsUiEvent
    data object CancelClicked : InvoiceDetailsUiEvent
    data object CancelConfirmed : InvoiceDetailsUiEvent
    data object RecordPaymentClicked : InvoiceDetailsUiEvent
    data object PreviewPdfClicked : InvoiceDetailsUiEvent
    data object SharePdfClicked : InvoiceDetailsUiEvent
    data class PaymentAmountChanged(val amountInput: String) : InvoiceDetailsUiEvent
    data object PaymentSubmitClicked : InvoiceDetailsUiEvent
    data object PaymentDismissed : InvoiceDetailsUiEvent
}
