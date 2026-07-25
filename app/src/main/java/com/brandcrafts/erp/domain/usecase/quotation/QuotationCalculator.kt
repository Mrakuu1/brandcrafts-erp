package com.brandcrafts.erp.domain.usecase.quotation

import java.math.BigDecimal
import java.math.RoundingMode
import javax.inject.Inject

data class QuotationCalculationLine(val quantity:BigDecimal,val unitPrice:BigDecimal,val discountPercent:BigDecimal,val taxPercent:BigDecimal)
data class QuotationLineTotals(val subtotal:BigDecimal,val discount:BigDecimal,val taxable:BigDecimal,val tax:BigDecimal,val total:BigDecimal)
data class QuotationTotals(val subtotal:BigDecimal,val discount:BigDecimal,val taxable:BigDecimal,val tax:BigDecimal,val grandTotal:BigDecimal)
class QuotationCalculator @Inject constructor() {
 fun line(input:QuotationCalculationLine):QuotationLineTotals { validate(input); val raw=input.quantity.multiply(input.unitPrice);val subtotal=money(raw);val discount=money(raw.multiply(input.discountPercent).divide(HUNDRED,PRECISION,RoundingMode.HALF_UP));val taxable=money(raw.subtract(raw.multiply(input.discountPercent).divide(HUNDRED,PRECISION,RoundingMode.HALF_UP)));val tax=money(taxable.multiply(input.taxPercent).divide(HUNDRED,PRECISION,RoundingMode.HALF_UP));return QuotationLineTotals(subtotal,discount,taxable,tax,money(taxable.add(tax))) }
 fun totals(lines:List<QuotationCalculationLine>):QuotationTotals { require(lines.isNotEmpty());val calculated=lines.map(::line);return QuotationTotals(calculated.sumOf{it.subtotal},calculated.sumOf{it.discount},calculated.sumOf{it.taxable},calculated.sumOf{it.tax},calculated.sumOf{it.total}) }
 private fun validate(v:QuotationCalculationLine){require(v.quantity>BigDecimal.ZERO);require(v.unitPrice>=BigDecimal.ZERO);require(v.discountPercent in BigDecimal.ZERO..HUNDRED);require(v.taxPercent in BigDecimal.ZERO..HUNDRED)}
 private fun money(v:BigDecimal)=v.setScale(2,RoundingMode.HALF_UP)
 private companion object { val HUNDRED=BigDecimal("100"); const val PRECISION=16 }
}
