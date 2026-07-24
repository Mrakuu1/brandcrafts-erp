package com.brandcrafts.erp.core.result

sealed interface StockOutResult<out T> { data class Success<T>(val data: T) : StockOutResult<T>; data class Error(val error: StockOutError) : StockOutResult<Nothing> }
enum class StockOutError { UNAUTHORIZED, MATERIAL_NOT_FOUND, MATERIAL_INACTIVE, INSUFFICIENT_STOCK, VALIDATION_FAILED, NETWORK_UNAVAILABLE, UNKNOWN }
