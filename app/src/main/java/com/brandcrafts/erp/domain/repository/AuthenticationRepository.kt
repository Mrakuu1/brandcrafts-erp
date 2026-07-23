package com.brandcrafts.erp.domain.repository

import com.brandcrafts.erp.core.result.AppResult
import com.brandcrafts.erp.domain.model.AuthenticatedUser

interface AuthenticationRepository {
    suspend fun login(email: String, password: String): AppResult<AuthenticatedUser>
    suspend fun resetPassword(email: String): AppResult<Unit>
    suspend fun logout(): AppResult<Unit>
    suspend fun getCurrentUser(): AppResult<AuthenticatedUser?>
}
