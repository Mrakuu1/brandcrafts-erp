package com.brandcrafts.erp.domain.repository

import com.brandcrafts.erp.domain.model.Contact
import com.brandcrafts.erp.domain.model.ContactInput
import com.brandcrafts.erp.domain.model.ContactUpdate
import com.brandcrafts.erp.core.result.ContactResult
import kotlinx.coroutines.flow.Flow

interface ContactRepository {
    fun observeContacts(): Flow<Result<List<Contact>>>
    suspend fun getContact(id: String): ContactResult<Contact>
    suspend fun createContact(input: ContactInput): ContactResult<Unit>
    suspend fun updateContact(input: ContactUpdate): ContactResult<Unit>
}
