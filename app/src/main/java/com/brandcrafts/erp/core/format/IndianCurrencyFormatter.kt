package com.brandcrafts.erp.core.format

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

private val IndiaLocale = Locale("en", "IN")

/** Formats every monetary value in the application as Indian rupees. */
fun formatIndianCurrency(value: BigDecimal): String =
    NumberFormat.getCurrencyInstance(IndiaLocale).format(value)
