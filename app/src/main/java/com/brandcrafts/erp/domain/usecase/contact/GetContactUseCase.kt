package com.brandcrafts.erp.domain.usecase.contact

import com.brandcrafts.erp.domain.repository.ContactRepository
import javax.inject.Inject

class GetContactUseCase @Inject constructor(
    private val repository: ContactRepository,
) {
    suspend operator fun invoke(id: String) = repository.getContact(id)
}
