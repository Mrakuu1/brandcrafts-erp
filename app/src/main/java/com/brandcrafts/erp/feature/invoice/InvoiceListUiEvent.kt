package com.brandcrafts.erp.feature.invoice

sealed interface InvoiceListUiEvent {
    data object Refresh : InvoiceListUiEvent
    data object Retry : InvoiceListUiEvent
    data class SearchChanged(val query: String) : InvoiceListUiEvent
    data class DocumentStatusFilterChanged(val filter: InvoiceDocumentStatusFilter) : InvoiceListUiEvent
    data class PaymentStatusFilterChanged(val filter: InvoicePaymentStatusFilter) : InvoiceListUiEvent
    data object CreateClicked : InvoiceListUiEvent
    data class DetailsClicked(val invoiceId: String) : InvoiceListUiEvent
    data class EditClicked(val invoiceId: String) : InvoiceListUiEvent
    data class IssueClicked(val invoiceId: String) : InvoiceListUiEvent
    data object IssueConfirmed : InvoiceListUiEvent
    data class CancelClicked(val invoiceId: String) : InvoiceListUiEvent
    data object CancelConfirmed : InvoiceListUiEvent
    data class RecordPaymentClicked(val invoiceId: String) : InvoiceListUiEvent
}
