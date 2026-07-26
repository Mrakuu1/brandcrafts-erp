package com.brandcrafts.erp.feature.invoice

data class InvoiceDetailsUiState(
    val content: InvoiceDetailsContent = InvoiceDetailsContent.Loading,
    val invoice: InvoiceDetailsModel? = null,
    val canEdit: Boolean = false,
    val canIssue: Boolean = false,
    val canCancel: Boolean = false,
    val canRecordPayment: Boolean = false,
    val operationInProgress: InvoiceDetailsOperation? = null,
    val isPdfGenerating: Boolean = false,
    val paymentEntry: InvoicePaymentEntryUiState? = null,
)

sealed interface InvoiceDetailsContent {
    data object Loading : InvoiceDetailsContent
    data object Loaded : InvoiceDetailsContent
    data class Error(val isRecoverable: Boolean = true) : InvoiceDetailsContent
}
