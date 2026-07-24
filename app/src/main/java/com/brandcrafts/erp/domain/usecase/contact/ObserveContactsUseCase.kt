package com.brandcrafts.erp.domain.usecase.contact

import com.brandcrafts.erp.domain.repository.ContactRepository
import javax.inject.Inject

class ObserveContactsUseCase @Inject constructor(
    private val repository: ContactRepository,
) {
    operator fun invoke() = repository.observeContacts()
}
