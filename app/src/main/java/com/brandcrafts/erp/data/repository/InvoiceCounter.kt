package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.core.result.InvoiceError
import com.brandcrafts.erp.core.result.InvoiceFailure

internal object InvoiceCounter {
    const val COLLECTION = "counters"; const val DOCUMENT = "invoice"
    fun current(value: Any?): Long = when (value) { null -> 0L; is Long -> value; is Int -> value.toLong(); is String -> value.toLongOrNull() ?: invalid(); else -> invalid() }.also { if (it < 0) invalid() }
    fun next(current: Long): Long = try { Math.addExact(current, 1L).also { if (it <= 0) invalid() } } catch (_: ArithmeticException) { throw InvoiceFailure(InvoiceError.CounterValueOverflow) }
    fun format(value: Long): String { if (value <= 0) invalid(); return "INV-" + value.toString().padStart(6, '0') }
    fun isValidNumber(value: String): Boolean = Regex("INV-[0-9]{6,}").matches(value)
    private fun invalid(): Nothing = throw InvoiceFailure(InvoiceError.CounterValueMalformed)
}
