package com.brandcrafts.erp.domain.usecase.contact

import com.brandcrafts.erp.domain.model.ContactInput
import com.brandcrafts.erp.domain.repository.ContactRepository
import javax.inject.Inject

class CreateContactUseCase @Inject constructor(
    private val repository: ContactRepository,
) {
    suspend operator fun invoke(input: ContactInput) = repository.createContact(input)
}
