package com.brandcrafts.erp.feature.invoice

import androidx.annotation.StringRes

sealed interface InvoiceDetailsUiEffect {
    data object NavigateBack : InvoiceDetailsUiEffect
    data class NavigateEditDraft(val invoiceId: String) : InvoiceDetailsUiEffect
    data object ConfirmIssue : InvoiceDetailsUiEffect
    data object ConfirmCancellation : InvoiceDetailsUiEffect
    data class PreviewPdf(val cacheFileName: String) : InvoiceDetailsUiEffect
    data class SharePdf(val cacheFileName: String) : InvoiceDetailsUiEffect
    data class ShowMessage(@StringRes val messageRes: Int) : InvoiceDetailsUiEffect
    data object Unauthorized : InvoiceDetailsUiEffect
}
