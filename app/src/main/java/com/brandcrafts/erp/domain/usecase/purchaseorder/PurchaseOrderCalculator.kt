package com.brandcrafts.erp.domain.usecase.purchaseorder

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

class PurchaseOrderCalculator @Inject constructor() {
    fun lineTotal(quantity: BigDecimal, unitPrice: BigDecimal): BigDecimal {
        require(quantity > BigDecimal.ZERO)
        require(unitPrice >= BigDecimal.ZERO)
        return quantity.multiply(unitPrice).setScale(2, RoundingMode.HALF_UP)
    }

    fun total(lines: List<PurchaseOrderCalculationLine>): BigDecimal {
        require(lines.isNotEmpty())
        return lines.fold(BigDecimal.ZERO) { total, line -> total + lineTotal(line.quantity, line.unitPrice) }
            .setScale(2, RoundingMode.HALF_UP)
    }
}

data class PurchaseOrderCalculationLine(val quantity: BigDecimal, val unitPrice: BigDecimal)
