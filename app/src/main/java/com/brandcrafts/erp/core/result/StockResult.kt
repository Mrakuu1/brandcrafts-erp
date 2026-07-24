package com.brandcrafts.erp.core.result

sealed interface StockResult<out T> {
    data class Success<T>(val data: T) : StockResult<T>
    data class Error(val error: StockError) : StockResult<Nothing>
}

enum class StockError { UNAUTHORIZED, MATERIAL_NOT_FOUND, MATERIAL_INACTIVE, VALIDATION_FAILED, NETWORK_UNAVAILABLE, UNKNOWN }
