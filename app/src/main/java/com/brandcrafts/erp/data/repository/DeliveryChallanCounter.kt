package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.core.result.DeliveryChallanError
import com.brandcrafts.erp.core.result.DeliveryChallanFailure

object DeliveryChallanCounter {
    const val COUNTER_DOCUMENT_PATH = "counters/deliveryChallan"
    private const val PREFIX = "DC-"
    private const val WIDTH = 6

    fun currentValue(storedValue: Any?): Long = when (storedValue) {
        null -> 0L
        is Long -> storedValue
        is Number -> storedValue.toLong()
        else -> throw DeliveryChallanFailure(DeliveryChallanError.CounterValueMalformed)
    }.takeIf { it >= 0L } ?: throw DeliveryChallanFailure(DeliveryChallanError.CounterValueMalformed)

    fun nextValue(currentValue: Long): Long {
        if (currentValue < 0L || currentValue == Long.MAX_VALUE) {
            throw DeliveryChallanFailure(DeliveryChallanError.CounterValueOverflow)
        }
        return currentValue + 1L
    }

    fun format(value: Long): String {
        if (value <= 0L || value > 999_999L) throw DeliveryChallanFailure(DeliveryChallanError.CounterValueOverflow)
        return PREFIX + value.toString().padStart(WIDTH, '0')
    }
}
