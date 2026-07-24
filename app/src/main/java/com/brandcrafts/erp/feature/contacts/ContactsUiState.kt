package com.brandcrafts.erp.feature.contacts
import com.brandcrafts.erp.domain.model.Contact
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.model.UserRole
data class ContactsUiState(val content:Content=Content.Loading,val selectedType:ContactType=ContactType.CUSTOMER,val searchQuery:String="",val allContacts:List<Contact> = emptyList(),val visibleContacts:List<Contact> = emptyList(),val role:UserRole?=null){sealed interface Content{data object Loading:Content;data object Loaded:Content;data object Empty:Content;data object Error:Content}}
