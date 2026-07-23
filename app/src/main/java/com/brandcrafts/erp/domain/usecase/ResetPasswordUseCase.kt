package com.brandcrafts.erp.domain.usecase

import com.brandcrafts.erp.domain.repository.AuthenticationRepository
import javax.inject.Inject

class ResetPasswordUseCase @Inject constructor(
    private val repository: AuthenticationRepository,
) {
    suspend operator fun invoke(email: String) = repository.resetPassword(email)
}
