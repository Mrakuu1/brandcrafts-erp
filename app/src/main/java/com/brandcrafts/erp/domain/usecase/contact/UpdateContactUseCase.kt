package com.brandcrafts.erp.domain.usecase.contact

import com.brandcrafts.erp.domain.model.ContactUpdate
import com.brandcrafts.erp.domain.repository.ContactRepository
import javax.inject.Inject

class UpdateContactUseCase @Inject constructor(
    private val repository: ContactRepository,
) {
    suspend operator fun invoke(input: ContactUpdate) = repository.updateContact(input)
}
