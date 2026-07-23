package com.brandcrafts.erp.core.result

sealed interface AppResult<out T> {
    data class Success<T>(val data: T) : AppResult<T>
    data class Error(val error: AuthenticationError) : AppResult<Nothing>
}

enum class AuthenticationError { INVALID_CREDENTIALS, ACCOUNT_DISABLED, USER_PROFILE_MISSING, NETWORK_UNAVAILABLE, UNKNOWN }
