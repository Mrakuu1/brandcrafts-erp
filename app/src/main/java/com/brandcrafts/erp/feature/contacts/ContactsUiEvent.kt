package com.brandcrafts.erp.feature.contacts
import com.brandcrafts.erp.domain.model.Contact
import com.brandcrafts.erp.domain.model.ContactType
sealed interface ContactsUiEvent{data class SearchChanged(val query:String):ContactsUiEvent;data class TypeSelected(val type:ContactType):ContactsUiEvent;data object AddClicked:ContactsUiEvent;data class ContactClicked(val contact:Contact):ContactsUiEvent;data class EditClicked(val contact:Contact):ContactsUiEvent;data object RetryClicked:ContactsUiEvent;data object ErrorConsumed:ContactsUiEvent}
