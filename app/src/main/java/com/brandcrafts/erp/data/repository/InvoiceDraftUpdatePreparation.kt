package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.core.result.InvoiceError
import com.brandcrafts.erp.core.result.InvoiceFailure
import com.brandcrafts.erp.domain.model.InvoicePaymentStatus
import com.brandcrafts.erp.domain.model.InvoiceStatus
import java.math.BigDecimal

data class ExistingInvoiceLineSnapshot(val lineId: String, val sortOrder: Int)
data class ExistingInvoiceDraftSnapshot(val invoiceId: String, val invoiceNumber: String, val status: InvoiceStatus, val paidAmount: BigDecimal, val paymentStatus: InvoicePaymentStatus, val createdAtMillis: Long?, val createdBy: String, val existingLines: List<ExistingInvoiceLineSnapshot>)
data class PreparedInvoiceDraftUpdateParent(val customerId: String, val invoiceDateMillis: Long, val dueDateMillis: Long?, val subtotal: BigDecimal, val discountTotal: BigDecimal, val taxTotal: BigDecimal, val grandTotal: BigDecimal, val remarks: String, val updatedBy: String)
data class InvoiceDraftUpdatePreparation(val invoiceId: String, val invoiceNumber: String, val actor: InvoiceValidatedActor, val customer: InvoiceValidatedCustomer, val parent: PreparedInvoiceDraftUpdateParent, val submittedLines: List<PreparedInvoiceLine>, val staleLineIds: List<String>, val activity: PreparedInvoiceActivity, val submittedItemCount: Int, val staleDeleteCount: Int, val requestedWriteCount: Int)

internal object InvoiceDraftUpdateIdentity {
    fun stale(existing: ExistingInvoiceDraftSnapshot, retained: List<String>): List<String> {
        if (existing.invoiceId.isBlank() || existing.invoiceNumber.isBlank() || existing.status != InvoiceStatus.DRAFT || existing.paidAmount != BigDecimal.ZERO || existing.paymentStatus != InvoicePaymentStatus.UNPAID) throw InvoiceFailure(InvoiceError.DraftOnlyUpdateRequired)
        if (retained.any { it.isBlank() } || retained.toSet().size != retained.size || !retained.all(existing.existingLines.map { it.lineId }.toSet()::contains)) throw InvoiceFailure(InvoiceError.InvalidLine)
        return existing.existingLines.map { it.lineId }.filterNot(retained::contains)
    }
}

class InvoiceDraftUpdatePreparer(private val actorValidator: InvoiceActorValidator, private val customerValidator: InvoiceCustomerValidator, private val validator: com.brandcrafts.erp.domain.usecase.invoice.InvoiceValidator, private val calculator: com.brandcrafts.erp.domain.usecase.invoice.InvoiceCalculator, private val ids: InvoiceCreateIdGenerator) {
    suspend fun prepare(request: com.brandcrafts.erp.domain.model.InvoiceDraftUpdateRequest, existing: ExistingInvoiceDraftSnapshot): InvoiceDraftUpdatePreparation {
        val actor = actorValidator.requireAdmin()
        if (request.invoiceId.isBlank() || request.invoiceId != existing.invoiceId || !InvoiceCounter.isValidNumber(existing.invoiceNumber)) throw InvoiceFailure(InvoiceError.DraftOnlyUpdateRequired)
        val retained = request.lines.mapNotNull { it.persistedLineId }.also { if (it.any(String::isBlank)) throw InvoiceFailure(InvoiceError.InvalidLine) }
        val stale = InvoiceDraftUpdateIdentity.stale(existing, retained)
        val newIds = ids.generateLineIds(existing.invoiceId, request.lines.count { it.persistedLineId == null })
        var nextNew = 0
        val raw = request.lines.mapIndexed { index, line -> com.brandcrafts.erp.domain.model.InvoiceLine(line.persistedLineId ?: newIds[nextNew++], line.materialId, line.description, line.quantity, line.unit, line.unitPrice, line.discountPercent, line.taxPercent, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, index) }
        validator.validateCreateInput(request.customerId, request.invoiceDateMillis, request.dueDateMillis, raw).getOrElse { throw it }
        val customer = customerValidator.requireActiveCustomer(request.customerId); val totals = calculator.calculate(raw).getOrElse { throw it }
        InvoiceWritePolicy.validateUpdate(totals.lines.size, stale.size)
        val lines = totals.lines.mapIndexed { i, line ->
            PreparedInvoiceLine(
                line.id, line.materialId, line.description, line.quantity, line.unit,
                line.unitPrice, line.discountPercent, line.taxPercent, line.lineSubtotal,
                line.lineDiscount, line.taxableAmount, line.lineTax, line.lineTotal, i,
            )
        }
        val parent = PreparedInvoiceDraftUpdateParent(customer.customerId, request.invoiceDateMillis, request.dueDateMillis, totals.subtotal, totals.discountTotal, totals.taxTotal, totals.grandTotal, request.remarks, actor.userId)
        val activity = PreparedInvoiceActivity(ids.generateActivityId(), "INVOICE_UPDATED", existing.invoiceId, actor.userId, actor.displayName, existing.invoiceNumber)
        return InvoiceDraftUpdatePreparation(existing.invoiceId, existing.invoiceNumber, actor, customer, parent, lines, stale, activity, lines.size, stale.size, InvoiceWritePolicy.updateWriteCount(lines.size, stale.size))
    }
}
