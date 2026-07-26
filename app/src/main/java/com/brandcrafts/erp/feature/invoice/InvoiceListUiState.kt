package com.brandcrafts.erp.feature.invoice

import com.brandcrafts.erp.domain.model.InvoicePaymentStatus
import com.brandcrafts.erp.domain.model.InvoiceStatus

data class InvoiceListUiState(
    val content: InvoiceListContent = InvoiceListContent.Loading,
    val searchQuery: String = "",
    val documentStatusFilter: InvoiceDocumentStatusFilter = InvoiceDocumentStatusFilter.All,
    val paymentStatusFilter: InvoicePaymentStatusFilter = InvoicePaymentStatusFilter.All,
    val rows: List<InvoiceListItem> = emptyList(),
    val canCreate: Boolean = false,
    val isRefreshing: Boolean = false,
    val actionInProgress: InvoiceListOperation? = null,
)

sealed interface InvoiceListContent {
    data object Loading : InvoiceListContent
    data object Loaded : InvoiceListContent
    data object Empty : InvoiceListContent
    data class Error(val isRecoverable: Boolean = true) : InvoiceListContent
}

sealed interface InvoiceDocumentStatusFilter {
    data object All : InvoiceDocumentStatusFilter
    data class Status(val value: InvoiceStatus) : InvoiceDocumentStatusFilter
}

sealed interface InvoicePaymentStatusFilter {
    data object All : InvoicePaymentStatusFilter
    data class Status(val value: InvoicePaymentStatus) : InvoicePaymentStatusFilter
}
