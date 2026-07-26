package com.brandcrafts.erp.data.mapper

import com.brandcrafts.erp.core.result.PurchaseOrderError
import com.brandcrafts.erp.core.result.PurchaseOrderFailure
import com.google.firebase.Timestamp
import java.util.Date

internal fun purchaseOrderDateTimestamp(value: Long): Timestamp {
    if (value <= 0L) throw PurchaseOrderFailure(PurchaseOrderError.PurchaseOrderDateMissing)
    return Timestamp(Date(value))
}

internal fun purchaseOrderOptionalDateTimestamp(value: Long?): Timestamp? = value?.let(::purchaseOrderDateTimestamp)

internal fun Any?.toPurchaseOrderDateMillis(required: Boolean): Long? = when (this) {
    null -> if (required) throw PurchaseOrderFailure(PurchaseOrderError.PurchaseOrderDateMissing) else null
    is Timestamp -> toDate().time.takeIf { it > 0L } ?: throw PurchaseOrderFailure(PurchaseOrderError.MalformedStoredDate)
    is Long -> takeIf { it > 0L } ?: throw PurchaseOrderFailure(PurchaseOrderError.MalformedStoredDate)
    is Number -> toLong().takeIf { it > 0L } ?: throw PurchaseOrderFailure(PurchaseOrderError.MalformedStoredDate)
    is String -> toLongOrNull()?.takeIf { it > 0L } ?: throw PurchaseOrderFailure(PurchaseOrderError.MalformedStoredDate)
    else -> throw PurchaseOrderFailure(PurchaseOrderError.MalformedStoredDate)
}
