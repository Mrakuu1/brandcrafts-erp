package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.core.result.InvoiceError
import com.brandcrafts.erp.core.result.InvoiceFailure
import com.brandcrafts.erp.domain.model.InvoiceCreateRequest
import com.brandcrafts.erp.domain.model.InvoicePaymentStatus
import com.brandcrafts.erp.domain.model.InvoiceStatus
import com.brandcrafts.erp.domain.usecase.invoice.InvoiceCalculator
import com.brandcrafts.erp.domain.usecase.invoice.InvoiceValidator
import java.math.BigDecimal
import kotlinx.coroutines.CancellationException

class InvoiceCreatePreparer(private val actorValidator: InvoiceActorValidator, private val customerValidator: InvoiceCustomerValidator, private val validator: InvoiceValidator, private val calculator: InvoiceCalculator, private val idGenerator: InvoiceCreateIdGenerator) {
    suspend fun prepare(request: InvoiceCreateRequest): InvoiceCreatePreparation = try {
        val actor = actorValidator.requireAdmin(); val customer = customerValidator.requireActiveCustomer(request.customerId)
        validator.validateCreateInput(request.customerId, request.invoiceDateMillis, request.dueDateMillis, request.lines).getOrElse { throw it }
        val totals = calculator.calculate(request.lines).getOrElse { throw it }; InvoiceWritePolicy.validateCreate(totals.lines.size)
        val ids = idGenerator.generateCreateIds(totals.lines.size); if (ids.lineIds.size != totals.lines.size) throw InvoiceFailure(InvoiceError.InvalidGeneratedDocumentId)
        val parent = PreparedInvoiceParent(ids.invoiceId, customer.customerId, request.invoiceDateMillis, request.dueDateMillis, InvoiceStatus.DRAFT, totals.subtotal, totals.discountTotal, totals.taxTotal, totals.grandTotal, BigDecimal.ZERO.setScale(2), InvoicePaymentStatus.UNPAID, request.remarks, actor.userId, actor.userId)
        val lines = totals.lines.mapIndexed { index, line ->
            PreparedInvoiceLine(
                ids.lineIds[index], line.materialId, line.description, line.quantity, line.unit,
                line.unitPrice, line.discountPercent, line.taxPercent, line.lineSubtotal,
                line.lineDiscount, line.taxableAmount, line.lineTax, line.lineTotal, index,
            )
        }
        InvoiceCreatePreparation(actor, customer, parent, lines, PreparedInvoiceActivity(ids.activityId, "INVOICE_CREATED", ids.invoiceId, actor.userId, actor.displayName), lines.size, InvoiceWritePolicy.createWriteCount(lines.size))
    } catch (exception: CancellationException) { throw exception }
}
