package com.brandcrafts.erp.feature.invoice

import androidx.annotation.StringRes

sealed interface InvoiceFormUiEffect {
    data class Saved(val invoiceId: String) : InvoiceFormUiEffect
    data object NavigateBack : InvoiceFormUiEffect
    data object Unauthorized : InvoiceFormUiEffect
    data object EditingBlocked : InvoiceFormUiEffect
    data class ShowMessage(@StringRes val messageRes: Int) : InvoiceFormUiEffect
}
