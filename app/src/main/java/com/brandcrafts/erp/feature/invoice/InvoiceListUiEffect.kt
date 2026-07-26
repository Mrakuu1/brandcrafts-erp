package com.brandcrafts.erp.feature.invoice

import androidx.annotation.StringRes

sealed interface InvoiceListUiEffect {
    data object NavigateCreate : InvoiceListUiEffect
    data class NavigateDetails(val invoiceId: String) : InvoiceListUiEffect
    data class NavigateEditDraft(val invoiceId: String) : InvoiceListUiEffect
    data class NavigateRecordPayment(val invoiceId: String) : InvoiceListUiEffect
    data class ConfirmIssue(val invoiceId: String) : InvoiceListUiEffect
    data class ConfirmCancellation(val invoiceId: String) : InvoiceListUiEffect
    data class ShowMessage(@StringRes val messageRes: Int) : InvoiceListUiEffect
    data object Unauthorized : InvoiceListUiEffect
}
