package com.brandcrafts.erp.data.mapper

import com.brandcrafts.erp.core.result.InvoiceError
import com.brandcrafts.erp.core.result.InvoiceFailure
import com.brandcrafts.erp.data.model.invoice.InvoiceDto
import com.brandcrafts.erp.data.model.invoice.InvoiceLineDto
import com.brandcrafts.erp.domain.model.Invoice
import com.brandcrafts.erp.domain.model.InvoiceSummary
import com.brandcrafts.erp.domain.model.InvoiceLine
import com.brandcrafts.erp.domain.model.InvoicePaymentStatus
import com.brandcrafts.erp.domain.model.InvoiceStatus
import com.brandcrafts.erp.domain.usecase.invoice.InvoiceValidator
import com.google.firebase.Timestamp
import java.math.BigDecimal
import java.util.Date

fun Invoice.toDto() = InvoiceDto(id, number, customerId, Timestamp(Date(invoiceDateMillis)), dueDateMillis?.let { Timestamp(Date(it)) }, status.name, subtotal.toPlainString(), discountTotal.toPlainString(), taxTotal.toPlainString(), grandTotal.toPlainString(), paidAmount.toPlainString(), paymentStatus.name, remarks, createdAtMillis?.let { Timestamp(Date(it)) }, createdBy, updatedAtMillis?.let { Timestamp(Date(it)) }, updatedBy, issuedAtMillis?.let { Timestamp(Date(it)) }, issuedBy, cancelledAtMillis?.let { Timestamp(Date(it)) }, cancelledBy)
fun InvoiceLine.toDto() = InvoiceLineDto(id, materialId, description, quantity.toPlainString(), unit, unitPrice.toPlainString(), discountPercent.toPlainString(), taxPercent.toPlainString(), lineSubtotal.toPlainString(), lineDiscount.toPlainString(), taxableAmount.toPlainString(), lineTax.toPlainString(), lineTotal.toPlainString(), sortOrder)
fun InvoiceLineDto.toDomain(documentId: String): InvoiceLine = InvoiceLine(requireText(itemId ?: documentId), requireText(materialId), requireText(description), decimal(quantity), requireText(unit), decimal(unitPrice), decimal(discountPercent), decimal(taxPercent), decimal(lineSubtotal), decimal(lineDiscount), decimal(taxableAmount), decimal(lineTax), decimal(lineTotal), sortOrder ?: throw InvoiceFailure(InvoiceError.InvalidLine))
fun InvoiceDto.toDomain(lines: List<InvoiceLine>): Invoice {
    val invoice = Invoice(requireText(id), requireText(invoiceNumber), requireText(customerId), date(invoiceDate, true)!!, date(dueDate, false), status(status), decimal(subtotal), decimal(discountTotal), decimal(taxTotal), decimal(grandTotal), decimal(paidAmount), remarks.orEmpty(), createdAt?.toDate()?.time, updatedAt?.toDate()?.time, createdBy.orEmpty(), updatedBy.orEmpty(), issuedAt?.toDate()?.time, issuedBy.orEmpty(), cancelledAt?.toDate()?.time, cancelledBy.orEmpty(), lines)
    if (payment(paymentStatus) != invoice.paymentStatus) throw InvoiceFailure(InvoiceError.InvalidPaidAmount)
    InvoiceValidator().validateStoredInvoice(invoice).getOrElse { throw it }; return invoice
}
fun InvoiceDto.toSummaryDomain(): InvoiceSummary {
    val summary = InvoiceSummary(requireText(id), requireText(invoiceNumber), requireText(customerId), null, date(invoiceDate, true)!!, date(dueDate, false), status(status), payment(paymentStatus), decimal(grandTotal), decimal(paidAmount))
    if (summary.paymentStatus != com.brandcrafts.erp.domain.model.InvoicePaymentStatus.from(summary.paidAmount, summary.grandTotal)) throw InvoiceFailure(InvoiceError.InvalidPaidAmount)
    if (summary.outstandingAmount < BigDecimal.ZERO) throw InvoiceFailure(InvoiceError.InvalidPaidAmount)
    return summary
}
private fun requireText(value: String?): String = value?.takeIf { it.isNotBlank() } ?: throw InvoiceFailure(InvoiceError.InvalidLine)
private fun decimal(value: String?): BigDecimal = try { BigDecimal(value ?: throw InvoiceFailure(InvoiceError.MalformedStoredDecimal)) } catch (_: NumberFormatException) { throw InvoiceFailure(InvoiceError.MalformedStoredDecimal) }
private fun date(value: Any?, required: Boolean): Long? = when (value) { null -> if (required) throw InvoiceFailure(InvoiceError.MalformedStoredDate) else null; is Timestamp -> value.toDate().time.takeIf { it > 0 } ?: throw InvoiceFailure(InvoiceError.MalformedStoredDate); is Number -> value.toLong().takeIf { it > 0 } ?: throw InvoiceFailure(InvoiceError.MalformedStoredDate); is String -> value.toLongOrNull()?.takeIf { it > 0 } ?: throw InvoiceFailure(InvoiceError.MalformedStoredDate); else -> throw InvoiceFailure(InvoiceError.MalformedStoredDate) }
private fun status(value: String?): InvoiceStatus = try { InvoiceStatus.valueOf(value ?: "") } catch (_: IllegalArgumentException) { throw InvoiceFailure(InvoiceError.InvalidStoredStatus) }
private fun payment(value: String?): InvoicePaymentStatus = try { InvoicePaymentStatus.valueOf(value ?: "") } catch (_: IllegalArgumentException) { throw InvoiceFailure(InvoiceError.InvalidPaidAmount) }
