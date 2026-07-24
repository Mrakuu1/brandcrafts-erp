package com.brandcrafts.erp.data.datasource.contact

import com.brandcrafts.erp.data.model.contact.ContactDto
import kotlinx.coroutines.flow.Flow

interface ContactsRemoteDataSource {
    fun observeContacts(): Flow<List<ContactDto>>
    suspend fun getContact(id: String): ContactDto?
    suspend fun createContact(contact: ContactDto, performedByName: String)
    suspend fun updateContact(contact: ContactDto, performedByName: String)
}
