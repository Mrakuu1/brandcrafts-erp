package com.brandcrafts.erp.domain.usecase.invoice

import com.brandcrafts.erp.core.result.InvoiceError
import com.brandcrafts.erp.core.result.InvoiceFailure
import com.brandcrafts.erp.domain.model.Invoice
import com.brandcrafts.erp.domain.model.InvoicePaymentStatus
import com.brandcrafts.erp.domain.model.InvoiceStatus
import java.math.BigDecimal
import java.math.RoundingMode

data class InvoicePaymentValidationResult(
    val paidAmount: BigDecimal,
    val outstandingAmount: BigDecimal,
    val paymentStatus: InvoicePaymentStatus,
)

class InvoiceValidator(private val calculator: InvoiceCalculator = InvoiceCalculator()) {
    fun validateCreateInput(customerId: String, invoiceDateMillis: Long, dueDateMillis: Long?, lines: List<com.brandcrafts.erp.domain.model.InvoiceLine>): Result<Unit> {
        if (customerId.isBlank()) return Result.failure(InvoiceFailure(InvoiceError.CustomerRequired))
        if (invoiceDateMillis <= 0L) return Result.failure(InvoiceFailure(InvoiceError.InvoiceDateRequired))
        if (dueDateMillis != null && dueDateMillis < invoiceDateMillis) return Result.failure(InvoiceFailure(InvoiceError.DueDateBeforeInvoiceDate))
        return calculator.calculate(lines).map { Unit }
    }
    fun validateCreate(invoice: Invoice): Result<InvoiceCalculationResult> = validateEditable(invoice, requireId = false)

    fun validateDraftUpdate(invoice: Invoice): Result<InvoiceCalculationResult> {
        if (invoice.id.isBlank()) return Result.failure(InvoiceFailure(InvoiceError.InvoiceNotFound))
        if (invoice.status != InvoiceStatus.DRAFT) return Result.failure(InvoiceFailure(InvoiceError.DraftOnlyUpdateRequired))
        return validateEditable(invoice, requireId = true)
    }

    fun validateIssue(invoice: Invoice): Result<InvoiceCalculationResult> {
        if (invoice.status != InvoiceStatus.DRAFT) return Result.failure(InvoiceFailure(InvoiceError.InvalidStatusTransition))
        return validateEditable(invoice, requireId = true)
    }

    fun validateCancellation(invoice: Invoice): Result<Unit> = when (invoice.status) {
        InvoiceStatus.DRAFT -> validatePaymentData(invoice.paidAmount, invoice.grandTotal).map { Unit }
        InvoiceStatus.ISSUED -> when {
            invoice.paidAmount > BigDecimal.ZERO -> Result.failure(InvoiceFailure(InvoiceError.InvoiceHasRecordedPayments))
            else -> validatePaymentData(invoice.paidAmount, invoice.grandTotal).map { Unit }
        }
        InvoiceStatus.CANCELLED -> Result.failure(InvoiceFailure(InvoiceError.InvalidStatusTransition))
    }

    fun validatePaymentRecording(invoice: Invoice, paymentAmount: BigDecimal): Result<InvoicePaymentValidationResult> {
        if (invoice.status != InvoiceStatus.ISSUED) return Result.failure(InvoiceFailure(InvoiceError.PaymentNotAllowedForCurrentStatus))
        if (paymentAmount <= BigDecimal.ZERO) return Result.failure(InvoiceFailure(InvoiceError.InvalidPaidAmount))
        return validatePaymentData(invoice.paidAmount, invoice.grandTotal).mapCatching { current ->
            val next = money(current.paidAmount.add(paymentAmount))
            if (next > invoice.grandTotal) throw InvoiceFailure(InvoiceError.PaymentExceedsOutstandingAmount)
            InvoicePaymentValidationResult(next, money(invoice.grandTotal.subtract(next)), derivePaymentStatus(next, invoice.grandTotal))
        }
    }

    fun derivePaymentStatus(paidAmount: BigDecimal, grandTotal: BigDecimal): InvoicePaymentStatus {
        validatePaymentData(paidAmount, grandTotal).getOrElse { throw it }
        return InvoicePaymentStatus.from(paidAmount, grandTotal)
    }

    fun validateStoredInvoice(invoice: Invoice): Result<Unit> {
        if (invoice.invoiceDateMillis <= 0L || (invoice.dueDateMillis != null && invoice.dueDateMillis < invoice.invoiceDateMillis)) {
            return Result.failure(InvoiceFailure(InvoiceError.MalformedStoredDate))
        }
        return calculator.calculate(invoice.lines).fold(
            onSuccess = { calculated ->
                if (calculated.subtotal != invoice.subtotal || calculated.discountTotal != invoice.discountTotal ||
                    calculated.taxTotal != invoice.taxTotal || calculated.grandTotal != invoice.grandTotal
                ) {
                    Result.failure(InvoiceFailure(InvoiceError.MalformedStoredDecimal))
                } else {
                    validatePaymentData(invoice.paidAmount, invoice.grandTotal).map { Unit }
                }
            },
            onFailure = { Result.failure(it) },
        )
    }

    private fun validateEditable(invoice: Invoice, requireId: Boolean): Result<InvoiceCalculationResult> {
        if (requireId && invoice.id.isBlank()) return Result.failure(InvoiceFailure(InvoiceError.InvoiceNotFound))
        if (invoice.customerId.isBlank()) return Result.failure(InvoiceFailure(InvoiceError.CustomerRequired))
        if (invoice.invoiceDateMillis <= 0L) return Result.failure(InvoiceFailure(InvoiceError.InvoiceDateRequired))
        if (invoice.dueDateMillis != null && invoice.dueDateMillis < invoice.invoiceDateMillis) return Result.failure(InvoiceFailure(InvoiceError.DueDateBeforeInvoiceDate))
        if (invoice.paidAmount != BigDecimal.ZERO || invoice.paymentStatus != InvoicePaymentStatus.UNPAID) return Result.failure(InvoiceFailure(InvoiceError.InvalidPaidAmount))
        return calculator.calculate(invoice.lines)
    }

    private fun validatePaymentData(paidAmount: BigDecimal, grandTotal: BigDecimal): Result<InvoicePaymentValidationResult> {
        if (paidAmount < BigDecimal.ZERO || grandTotal < BigDecimal.ZERO) return Result.failure(InvoiceFailure(InvoiceError.InvalidPaidAmount))
        if (paidAmount > grandTotal) return Result.failure(InvoiceFailure(InvoiceError.PaymentExceedsOutstandingAmount))
        return Result.success(InvoicePaymentValidationResult(paidAmount, money(grandTotal.subtract(paidAmount)), InvoicePaymentStatus.from(paidAmount, grandTotal)))
    }

    private fun money(value: BigDecimal): BigDecimal = value.setScale(2, RoundingMode.HALF_UP)
}
