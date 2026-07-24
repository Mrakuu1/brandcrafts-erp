package com.brandcrafts.erp.core.result

sealed interface ContactResult<out T> {
    data class Success<T>(val data: T) : ContactResult<T>
    data class Error(val error: ContactError) : ContactResult<Nothing>
}

enum class ContactError {
    UNAUTHORIZED,
    VALIDATION_FAILED,
    DUPLICATE_PHONE,
    DUPLICATE_EMAIL,
    CONTACT_NOT_FOUND,
    NETWORK_UNAVAILABLE,
    UNKNOWN,
}
