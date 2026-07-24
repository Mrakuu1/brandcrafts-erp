package com.brandcrafts.erp.feature.contacts

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.core.result.ContactError
import com.brandcrafts.erp.core.result.ContactResult
import com.brandcrafts.erp.core.validation.EmailValidator
import com.brandcrafts.erp.domain.model.Contact
import com.brandcrafts.erp.domain.model.ContactInput
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.model.ContactUpdate
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.domain.usecase.contact.CreateContactUseCase
import com.brandcrafts.erp.domain.usecase.contact.GetContactUseCase
import com.brandcrafts.erp.domain.usecase.contact.UpdateContactUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ContactFormViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getContact: GetContactUseCase,
    private val createContact: CreateContactUseCase,
    private val updateContact: UpdateContactUseCase,
    private val sessionManager: SessionManager,
) : ViewModel() {
    private val routeMode = savedStateHandle.get<String>(CONTACT_FORM_MODE_ARGUMENT)
    private val contactId = savedStateHandle.get<String>(CONTACT_FORM_ID_ARGUMENT)
        ?.takeUnless { it == CONTACT_FORM_EMPTY_ARGUMENT }
    private val requestedType = savedStateHandle.get<String>(CONTACT_FORM_TYPE_ARGUMENT)
        ?.uppercase()
        ?.let { value -> ContactType.entries.firstOrNull { it.name == value } }

    private val mode = if (routeMode == CONTACT_FORM_EDIT_MODE) ContactFormMode.EDIT else ContactFormMode.CREATE
    private val _uiState = MutableStateFlow(
        ContactFormUiState(mode = mode, contactId = contactId),
    )
    val uiState: StateFlow<ContactFormUiState> = _uiState.asStateFlow()

    private val effectsChannel = Channel<ContactFormUiEffect>(Channel.BUFFERED)
    val effects = effectsChannel.receiveAsFlow()

    init {
        when (mode) {
            ContactFormMode.CREATE -> initializeCreate(requestedType)
            ContactFormMode.EDIT -> contactId?.let(::loadContact) ?: setLoadError(ContactFormError.CONTACT_NOT_FOUND)
        }
    }

    fun onEvent(event: ContactFormUiEvent) {
        when (event) {
            is ContactFormUiEvent.NameChanged -> update { copy(name = event.value, errors = errors.copy(name = null), saveError = null) }
            is ContactFormUiEvent.CompanyChanged -> update { copy(company = event.value, saveError = null) }
            is ContactFormUiEvent.PhoneChanged -> update { copy(phone = event.value, errors = errors.copy(phone = null), saveError = null) }
            is ContactFormUiEvent.EmailChanged -> update { copy(email = event.value, errors = errors.copy(email = null), saveError = null) }
            is ContactFormUiEvent.AddressChanged -> update { copy(address = event.value, saveError = null) }
            is ContactFormUiEvent.GstNumberChanged -> update { copy(gstNumber = event.value, saveError = null) }
            is ContactFormUiEvent.CityChanged -> update { copy(city = event.value, saveError = null) }
            is ContactFormUiEvent.StateChanged -> update { copy(state = event.value, saveError = null) }
            is ContactFormUiEvent.PincodeChanged -> update { copy(pincode = event.value, saveError = null) }
            is ContactFormUiEvent.NotesChanged -> update { copy(notes = event.value, saveError = null) }
            is ContactFormUiEvent.ActiveChanged -> update { copy(active = event.value, saveError = null) }
            ContactFormUiEvent.SaveClicked -> save()
            ContactFormUiEvent.CancelClicked -> sendEffect(ContactFormUiEffect.NavigateBack)
            ContactFormUiEvent.RetryLoadClicked -> when (mode) {
                ContactFormMode.CREATE -> initializeCreate(requestedType)
                ContactFormMode.EDIT -> contactId?.let(::loadContact) ?: setLoadError(ContactFormError.CONTACT_NOT_FOUND)
            }
        }
    }

    private fun initializeCreate(type: ContactType?) {
        if (type == null || !canWrite(type)) {
            setLoadError(ContactFormError.UNAUTHORIZED)
            return
        }
        _uiState.value = ContactFormUiState(mode = ContactFormMode.CREATE, type = type, isInitialLoading = false)
    }

    private fun loadContact(id: String) {
        update { copy(isInitialLoading = true, loadError = null) }
        viewModelScope.launch {
            when (val result = getContact(id)) {
                is ContactResult.Success -> {
                    if (canWrite(result.data.type)) {
                        _uiState.value = result.data.toFormState()
                    } else {
                        setLoadError(ContactFormError.UNAUTHORIZED)
                    }
                }
                is ContactResult.Error -> setLoadError(result.error.toFormError())
            }
        }
    }

    private fun save() {
        val state = _uiState.value
        if (state.isInitialLoading || state.isSaving || state.loadError != null) return
        val errors = state.validate()
        if (errors != ContactFormFieldErrors()) {
            update { copy(errors = errors, saveError = null) }
            return
        }
        val type = state.type ?: run {
            setLoadError(ContactFormError.UNAUTHORIZED)
            return
        }
        if (!canWrite(type)) {
            update { copy(saveError = ContactFormError.UNAUTHORIZED) }
            return
        }

        update { copy(isSaving = true, saveError = null) }
        viewModelScope.launch {
            val current = _uiState.value
            val result = when (current.mode) {
                ContactFormMode.CREATE -> createContact(current.toInput(requireNotNull(current.type)))
                ContactFormMode.EDIT -> updateContact(current.toUpdate())
            }
            when (result) {
                is ContactResult.Success -> sendEffect(ContactFormUiEffect.ContactSaved(current.mode))
                is ContactResult.Error -> update { copy(isSaving = false, saveError = result.error.toFormError()) }
            }
        }
    }

    private fun ContactFormUiState.validate(): ContactFormFieldErrors = ContactFormFieldErrors(
        name = if (name.trim().isBlank()) R.string.contact_form_name_required else null,
        phone = when {
            phone.trim().isBlank() -> R.string.contact_form_phone_required
            !phone.isValidPhone() -> R.string.contact_form_phone_invalid
            else -> null
        },
        email = if (email.isNotBlank() && !EmailValidator.isValid(email)) R.string.login_email_invalid else null,
    )

    private fun String.isValidPhone(): Boolean {
        val trimmed = trim()
        val digits = trimmed.count(Char::isDigit)
        return digits in 7..15 && trimmed.all { it.isDigit() || it == '+' || it == '-' || it == '(' || it == ')' || it.isWhitespace() }
    }

    private fun ContactFormUiState.toInput(type: ContactType): ContactInput = ContactInput(
        type = type, name = name.trim(), company = company.trim(), phone = phone.trim(), email = email.trim(),
        address = address.trim(), gstNumber = gstNumber.trim(), city = city.trim(), state = state.trim(),
        pincode = pincode.trim(), notes = notes.trim(), active = active,
    )

    private fun ContactFormUiState.toUpdate(): ContactUpdate = ContactUpdate(
        id = requireNotNull(contactId), name = name.trim(), company = company.trim(), phone = phone.trim(), email = email.trim(),
        address = address.trim(), gstNumber = gstNumber.trim(), city = city.trim(), state = state.trim(),
        pincode = pincode.trim(), notes = notes.trim(), active = active,
    )

    private fun Contact.toFormState(): ContactFormUiState = ContactFormUiState(
        mode = ContactFormMode.EDIT, contactId = id, type = type, name = name, company = company, phone = phone,
        email = email, address = address, gstNumber = gstNumber, city = city, state = state, pincode = pincode,
        notes = notes, active = active, isInitialLoading = false,
    )

    private fun canWrite(type: ContactType): Boolean {
        val user = (sessionManager.currentUser.value as? CurrentUserState.Authenticated)?.user ?: return false
        return user.active && (user.role == UserRole.ADMIN || type == ContactType.CUSTOMER)
    }

    private fun ContactError.toFormError(): ContactFormError = when (this) {
        ContactError.UNAUTHORIZED -> ContactFormError.UNAUTHORIZED
        ContactError.DUPLICATE_PHONE -> ContactFormError.DUPLICATE_PHONE
        ContactError.DUPLICATE_EMAIL -> ContactFormError.DUPLICATE_EMAIL
        ContactError.CONTACT_NOT_FOUND -> ContactFormError.CONTACT_NOT_FOUND
        ContactError.NETWORK_UNAVAILABLE -> ContactFormError.NETWORK
        ContactError.VALIDATION_FAILED, ContactError.UNKNOWN -> ContactFormError.UNKNOWN
    }

    private fun setLoadError(error: ContactFormError) {
        update { copy(isInitialLoading = false, loadError = error) }
    }

    private fun update(transform: ContactFormUiState.() -> ContactFormUiState) {
        _uiState.value = _uiState.value.transform()
    }

    private fun sendEffect(effect: ContactFormUiEffect) {
        viewModelScope.launch { effectsChannel.send(effect) }
    }

    private companion object {
        const val CONTACT_FORM_MODE_ARGUMENT = "mode"
        const val CONTACT_FORM_ID_ARGUMENT = "contactId"
        const val CONTACT_FORM_TYPE_ARGUMENT = "contactType"
        const val CONTACT_FORM_EDIT_MODE = "edit"
        const val CONTACT_FORM_EMPTY_ARGUMENT = "_"
    }
}
