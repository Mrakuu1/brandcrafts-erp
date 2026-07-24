package com.brandcrafts.erp.feature.contacts

sealed interface ContactFormUiEffect {
    data object NavigateBack : ContactFormUiEffect
    data class ContactSaved(val mode: ContactFormMode) : ContactFormUiEffect
}
