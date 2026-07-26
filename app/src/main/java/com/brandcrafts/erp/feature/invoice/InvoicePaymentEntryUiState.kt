package com.brandcrafts.erp.feature.invoice

import com.brandcrafts.erp.domain.model.InvoicePaymentStatus
import java.math.BigDecimal

data class InvoicePaymentEntryUiState(
    val invoiceId: String,
    val grandTotal: BigDecimal,
    val paidAmount: BigDecimal,
    val outstandingAmount: BigDecimal,
    val paymentStatus: InvoicePaymentStatus,
    val amountInput: String = "",
    val amount: BigDecimal? = null,
    val amountError: InvoicePaymentEntryError? = null,
    val isSaving: Boolean = false,
)
