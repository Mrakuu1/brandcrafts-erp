package com.brandcrafts.erp.domain.usecase

import com.brandcrafts.erp.domain.repository.AuthenticationRepository
import javax.inject.Inject

class ObserveCurrentUserUseCase @Inject constructor(
    private val repository: AuthenticationRepository,
) {
    operator fun invoke() = repository.observeCurrentUser()
}
