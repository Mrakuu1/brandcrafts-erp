package com.brandcrafts.erp.feature.invoice

import com.brandcrafts.erp.domain.model.InvoicePaymentStatus
import com.brandcrafts.erp.domain.model.InvoiceStatus
import java.math.BigDecimal

data class InvoiceListItem(
    val id: String,
    val invoiceNumber: String,
    val customerId: String,
    val customerName: String?,
    val invoiceDateMillis: Long,
    val dueDateMillis: Long?,
    val status: InvoiceStatus,
    val paymentStatus: InvoicePaymentStatus,
    val grandTotal: BigDecimal,
    val paidAmount: BigDecimal,
    val outstandingAmount: BigDecimal,
    val isOverdue: Boolean,
    val canEdit: Boolean,
    val canIssue: Boolean,
    val canCancel: Boolean,
    val canRecordPayment: Boolean,
)

data class InvoiceCustomerOption(
    val id: String,
    val label: String,
)

data class InvoiceMaterialOption(
    val id: String,
    val name: String,
    val unit: String,
)

data class EditableInvoiceLine(
    val localId: String,
    val persistedLineId: String?,
    val materialId: String?,
    val description: String,
    val quantity: BigDecimal?,
    val unit: String,
    val unitPrice: BigDecimal?,
    val discountPercent: BigDecimal?,
    val taxPercent: BigDecimal?,
    val lineSubtotal: BigDecimal?,
    val lineDiscount: BigDecimal?,
    val taxableAmount: BigDecimal?,
    val lineTax: BigDecimal?,
    val lineTotal: BigDecimal?,
    val errors: InvoiceLineFieldErrors = InvoiceLineFieldErrors(),
)

data class InvoiceFormTotals(
    val subtotal: BigDecimal,
    val discountTotal: BigDecimal,
    val taxTotal: BigDecimal,
    val grandTotal: BigDecimal,
)

data class InvoiceDetailsModel(
    val id: String,
    val invoiceNumber: String,
    val customer: InvoiceCustomerOption,
    val invoiceDateMillis: Long,
    val dueDateMillis: Long?,
    val status: InvoiceStatus,
    val paymentStatus: InvoicePaymentStatus,
    val lines: List<InvoiceDetailsLine>,
    val subtotal: BigDecimal,
    val discountTotal: BigDecimal,
    val taxTotal: BigDecimal,
    val grandTotal: BigDecimal,
    val paidAmount: BigDecimal,
    val outstandingAmount: BigDecimal,
    val isOverdue: Boolean,
    val remarks: String,
    val createdAtMillis: Long?,
    val updatedAtMillis: Long?,
    val issuedAtMillis: Long?,
    val cancelledAtMillis: Long?,
)

data class InvoiceDetailsLine(
    val id: String,
    val materialId: String,
    val description: String,
    val quantity: BigDecimal,
    val unit: String,
    val unitPrice: BigDecimal,
    val discountPercent: BigDecimal,
    val taxPercent: BigDecimal,
    val lineSubtotal: BigDecimal,
    val lineDiscount: BigDecimal,
    val taxableAmount: BigDecimal,
    val lineTax: BigDecimal,
    val lineTotal: BigDecimal,
    val sortOrder: Int,
)

sealed interface InvoiceFormMode {
    data object Create : InvoiceFormMode
    data class EditDraft(val invoiceId: String) : InvoiceFormMode
}

enum class InvoiceFieldError {
    REQUIRED,
    INVALID_DECIMAL,
    INVALID_DATE,
    DUE_DATE_BEFORE_INVOICE_DATE,
    INVALID_QUANTITY,
    INVALID_UNIT,
    INVALID_UNIT_PRICE,
    OUT_OF_RANGE,
}

data class InvoiceLineFieldErrors(
    val material: InvoiceFieldError? = null,
    val description: InvoiceFieldError? = null,
    val quantity: InvoiceFieldError? = null,
    val unit: InvoiceFieldError? = null,
    val unitPrice: InvoiceFieldError? = null,
    val discountPercent: InvoiceFieldError? = null,
    val taxPercent: InvoiceFieldError? = null,
)

data class InvoiceFormFieldErrors(
    val customer: InvoiceFieldError? = null,
    val invoiceDate: InvoiceFieldError? = null,
    val dueDate: InvoiceFieldError? = null,
    val items: InvoiceFieldError? = null,
    val lines: Map<String, InvoiceLineFieldErrors> = emptyMap(),
)

enum class InvoiceFormError {
    LOAD,
    SAVE,
    EDITING_BLOCKED,
}

data class InvoiceListOperation(
    val invoiceId: String,
    val action: InvoiceListAction,
)

enum class InvoiceListAction {
    ISSUE,
    CANCEL,
}

enum class InvoiceDetailsOperation {
    ISSUE,
    CANCEL,
    RECORD_PAYMENT,
}

enum class InvoicePaymentEntryError {
    REQUIRED,
    INVALID_AMOUNT,
    EXCEEDS_OUTSTANDING,
}
