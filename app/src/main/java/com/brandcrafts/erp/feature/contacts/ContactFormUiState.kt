package com.brandcrafts.erp.feature.contacts

import androidx.annotation.StringRes
import com.brandcrafts.erp.domain.model.ContactType

enum class ContactFormMode { CREATE, EDIT }

data class ContactFormUiState(
    val mode: ContactFormMode = ContactFormMode.CREATE,
    val contactId: String? = null,
    val type: ContactType? = null,
    val name: String = "",
    val company: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val gstNumber: String = "",
    val city: String = "",
    val state: String = "",
    val pincode: String = "",
    val notes: String = "",
    val active: Boolean = true,
    val errors: ContactFormFieldErrors = ContactFormFieldErrors(),
    val isInitialLoading: Boolean = true,
    val isSaving: Boolean = false,
    val loadError: ContactFormError? = null,
    val saveError: ContactFormError? = null,
)

data class ContactFormFieldErrors(
    @param:StringRes val name: Int? = null,
    @param:StringRes val phone: Int? = null,
    @param:StringRes val email: Int? = null,
)

enum class ContactFormError {
    UNAUTHORIZED,
    DUPLICATE_PHONE,
    DUPLICATE_EMAIL,
    CONTACT_NOT_FOUND,
    NETWORK,
    UNKNOWN,
}
