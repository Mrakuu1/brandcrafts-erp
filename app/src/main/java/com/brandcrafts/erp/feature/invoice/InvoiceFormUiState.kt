package com.brandcrafts.erp.feature.invoice

data class InvoiceFormUiState(
    val mode: InvoiceFormMode,
    val invoiceNumber: String? = null,
    val isLoading: Boolean = false,
    val customerOptions: List<InvoiceCustomerOption> = emptyList(),
    val materialOptions: List<InvoiceMaterialOption> = emptyList(),
    val customerId: String? = null,
    val invoiceDateMillis: Long? = null,
    val dueDateMillis: Long? = null,
    val lines: List<EditableInvoiceLine> = emptyList(),
    val remarks: String = "",
    val totals: InvoiceFormTotals? = null,
    val errors: InvoiceFormFieldErrors = InvoiceFormFieldErrors(),
    val isSaving: Boolean = false,
    val formError: InvoiceFormError? = null,
    val isEditingBlocked: Boolean = false,
)
