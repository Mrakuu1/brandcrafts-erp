package com.brandcrafts.erp.domain.usecase.invoice

import com.brandcrafts.erp.core.result.InvoiceError
import com.brandcrafts.erp.core.result.InvoiceFailure
import com.brandcrafts.erp.domain.model.InvoiceLine
import java.math.BigDecimal
import java.math.RoundingMode

data class InvoiceCalculationResult(
    val lines: List<InvoiceLine>,
    val subtotal: BigDecimal,
    val discountTotal: BigDecimal,
    val taxableTotal: BigDecimal,
    val taxTotal: BigDecimal,
    val grandTotal: BigDecimal,
)

class InvoiceCalculator {
    fun calculate(lines: List<InvoiceLine>): Result<InvoiceCalculationResult> {
        return try {
        if (lines.isEmpty()) return Result.failure(InvoiceFailure(InvoiceError.EmptyItemList))
        val calculatedLines = lines.map(::calculateLine)
        Result.success(
            InvoiceCalculationResult(
                lines = calculatedLines,
                subtotal = money(calculatedLines.sumOf { it.lineSubtotal }),
                discountTotal = money(calculatedLines.sumOf { it.lineDiscount }),
                taxableTotal = money(calculatedLines.sumOf { it.taxableAmount }),
                taxTotal = money(calculatedLines.sumOf { it.lineTax }),
                grandTotal = money(calculatedLines.sumOf { it.lineTotal }),
            ),
        )
    } catch (failure: InvoiceFailure) {
        Result.failure(failure)
        }
    }

    fun calculateLine(line: InvoiceLine): InvoiceLine {
        validateLine(line)
        val subtotal = money(line.quantity.multiply(line.unitPrice))
        val discount = money(subtotal.multiply(line.discountPercent).divide(HUNDRED, PRECISION, RoundingMode.HALF_UP))
        val taxable = money(subtotal.subtract(discount))
        val tax = money(taxable.multiply(line.taxPercent).divide(HUNDRED, PRECISION, RoundingMode.HALF_UP))
        return line.copy(
            lineSubtotal = subtotal,
            lineDiscount = discount,
            taxableAmount = taxable,
            lineTax = tax,
            lineTotal = money(taxable.add(tax)),
        )
    }

    private fun validateLine(line: InvoiceLine) {
        if (line.id.isBlank() || line.materialId.isBlank() || line.description.isBlank()) throw InvoiceFailure(InvoiceError.InvalidLine)
        if (line.quantity <= BigDecimal.ZERO) throw InvoiceFailure(InvoiceError.InvalidQuantity)
        if (line.unit.isBlank()) throw InvoiceFailure(InvoiceError.InvalidUnit)
        if (line.unitPrice < BigDecimal.ZERO) throw InvoiceFailure(InvoiceError.InvalidUnitPrice)
        if (line.discountPercent !in BigDecimal.ZERO..HUNDRED || line.taxPercent !in BigDecimal.ZERO..HUNDRED) throw InvoiceFailure(InvoiceError.InvalidLine)
    }

    private fun money(value: BigDecimal): BigDecimal = value.setScale(2, RoundingMode.HALF_UP)

    private companion object {
        val HUNDRED = BigDecimal("100")
        const val PRECISION = 16
    }
}
