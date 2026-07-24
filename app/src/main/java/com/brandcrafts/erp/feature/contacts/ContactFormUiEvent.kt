package com.brandcrafts.erp.feature.contacts

sealed interface ContactFormUiEvent {
    data class NameChanged(val value: String) : ContactFormUiEvent
    data class CompanyChanged(val value: String) : ContactFormUiEvent
    data class PhoneChanged(val value: String) : ContactFormUiEvent
    data class EmailChanged(val value: String) : ContactFormUiEvent
    data class AddressChanged(val value: String) : ContactFormUiEvent
    data class GstNumberChanged(val value: String) : ContactFormUiEvent
    data class CityChanged(val value: String) : ContactFormUiEvent
    data class StateChanged(val value: String) : ContactFormUiEvent
    data class PincodeChanged(val value: String) : ContactFormUiEvent
    data class NotesChanged(val value: String) : ContactFormUiEvent
    data class ActiveChanged(val value: Boolean) : ContactFormUiEvent
    data object SaveClicked : ContactFormUiEvent
    data object CancelClicked : ContactFormUiEvent
    data object RetryLoadClicked : ContactFormUiEvent
}
