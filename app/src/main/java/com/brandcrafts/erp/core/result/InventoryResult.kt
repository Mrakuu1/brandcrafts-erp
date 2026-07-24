package com.brandcrafts.erp.core.result

sealed interface InventoryResult<out T> {
    data class Success<T>(val data: T) : InventoryResult<T>
    data class Error(val error: InventoryError) : InventoryResult<Nothing>
}

enum class InventoryError {
    UNAUTHORIZED,
    VALIDATION_FAILED,
    ITEM_NOT_FOUND,
    NETWORK_UNAVAILABLE,
    UNKNOWN,
}
