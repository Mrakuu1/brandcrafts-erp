package com.brandcrafts.erp.feature.invoice

import java.math.BigDecimal

sealed interface InvoiceFormUiEvent {
    data object Retry : InvoiceFormUiEvent
    data class CustomerSelected(val customerId: String) : InvoiceFormUiEvent
    data class InvoiceDateChanged(val invoiceDateMillis: Long?) : InvoiceFormUiEvent
    data class DueDateChanged(val dueDateMillis: Long?) : InvoiceFormUiEvent
    data class RemarksChanged(val remarks: String) : InvoiceFormUiEvent
    data object AddLine : InvoiceFormUiEvent
    data class RemoveLine(val localId: String) : InvoiceFormUiEvent
    data class MaterialSelected(val localId: String, val materialId: String) : InvoiceFormUiEvent
    data class LineChanged(
        val localId: String,
        val description: String? = null,
        val quantity: BigDecimal? = null,
        val unit: String? = null,
        val unitPrice: BigDecimal? = null,
        val discountPercent: BigDecimal? = null,
        val taxPercent: BigDecimal? = null,
    ) : InvoiceFormUiEvent
    data object Save : InvoiceFormUiEvent
    data object Back : InvoiceFormUiEvent
}
