package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.core.result.ContactError
import com.brandcrafts.erp.core.result.ContactResult
import com.brandcrafts.erp.core.validation.EmailValidator
import com.brandcrafts.erp.data.datasource.contact.ContactsRemoteDataSource
import com.brandcrafts.erp.data.datasource.contact.DuplicateEmailException
import com.brandcrafts.erp.data.datasource.contact.DuplicatePhoneException
import com.brandcrafts.erp.data.mapper.toContactDto
import com.brandcrafts.erp.data.mapper.toDomain
import com.brandcrafts.erp.domain.model.AuthenticatedUser
import com.brandcrafts.erp.domain.model.Contact
import com.brandcrafts.erp.domain.model.ContactInput
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.model.ContactUpdate
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.domain.repository.ContactRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class ContactRepositoryImpl @Inject constructor(
    private val remoteDataSource: ContactsRemoteDataSource,
    private val sessionManager: SessionManager,
) : ContactRepository {

    override fun observeContacts(): Flow<Result<List<Contact>>> = remoteDataSource.observeContacts()
        .map { contacts -> runCatching { contacts.map { it.toDomain() } } }
        .catch { emit(Result.failure(IllegalStateException("Unable to observe contacts"))) }

    override suspend fun getContact(id: String): ContactResult<Contact> {
        if (id.isBlank() || activeUser() == null) {
            return ContactResult.Error(ContactError.UNAUTHORIZED)
        }
        return runCatching { remoteDataSource.getContact(id)?.toDomain() }
            .fold(
                onSuccess = { contact ->
                    contact?.let { ContactResult.Success(it) }
                        ?: ContactResult.Error(ContactError.CONTACT_NOT_FOUND)
                },
                onFailure = { ContactResult.Error(it.toContactError()) },
            )
    }

    override suspend fun createContact(input: ContactInput): ContactResult<Unit> {
        if (!input.isValid()) return ContactResult.Error(ContactError.VALIDATION_FAILED)
        val user = authorizedUser(input.type) ?: return ContactResult.Error(ContactError.UNAUTHORIZED)

        return runCatching {
            remoteDataSource.createContact(input.normalized().toContactDto(user.uid), user.name)
        }.fold(
            onSuccess = { ContactResult.Success(Unit) },
            onFailure = { ContactResult.Error(it.toContactError()) },
        )
    }

    override suspend fun updateContact(input: ContactUpdate): ContactResult<Unit> {
        if (!input.isValid()) return ContactResult.Error(ContactError.VALIDATION_FAILED)
        val user = activeUser() ?: return ContactResult.Error(ContactError.UNAUTHORIZED)

        return runCatching {
            val existing = remoteDataSource.getContact(input.id)
                ?: throw ContactNotFoundException
            val existingType = existing.toDomain().type
            if (!user.canWrite(existingType)) throw ContactUnauthorizedException
            remoteDataSource.updateContact(input.normalized().toContactDto(user.uid), user.name)
        }.fold(
            onSuccess = { ContactResult.Success(Unit) },
            onFailure = { ContactResult.Error(it.toContactError()) },
        )
    }

    private fun activeUser(): AuthenticatedUser? =
        (sessionManager.currentUser.value as? CurrentUserState.Authenticated)
            ?.user
            ?.takeIf(AuthenticatedUser::active)

    private fun authorizedUser(type: ContactType): AuthenticatedUser? =
        activeUser()?.takeIf { it.canWrite(type) }

    private fun AuthenticatedUser.canWrite(type: ContactType): Boolean =
        role == UserRole.ADMIN || type == ContactType.CUSTOMER

    private fun ContactInput.isValid(): Boolean = name.isNotBlank() &&
        phone.isNotBlank() &&
        (email.isBlank() || email.isValidEmail())

    private fun ContactUpdate.isValid(): Boolean = id.isNotBlank() &&
        name.isNotBlank() &&
        phone.isNotBlank() &&
        (email.isBlank() || email.isValidEmail())

    private fun String.isValidEmail(): Boolean = EmailValidator.isValid(this)

    private fun ContactInput.normalized(): ContactInput = copy(
        name = name.trim(), company = company.trim(), phone = phone.trim(), email = email.trim(),
        address = address.trim(), gstNumber = gstNumber.trim(), city = city.trim(), state = state.trim(),
        pincode = pincode.trim(), notes = notes.trim(),
    )

    private fun ContactUpdate.normalized(): ContactUpdate = copy(
        id = id.trim(), name = name.trim(), company = company.trim(), phone = phone.trim(), email = email.trim(),
        address = address.trim(), gstNumber = gstNumber.trim(), city = city.trim(), state = state.trim(),
        pincode = pincode.trim(), notes = notes.trim(),
    )

    private fun Throwable.toContactError(): ContactError = when (this) {
        DuplicatePhoneException -> ContactError.DUPLICATE_PHONE
        DuplicateEmailException -> ContactError.DUPLICATE_EMAIL
        ContactNotFoundException -> ContactError.CONTACT_NOT_FOUND
        ContactUnauthorizedException -> ContactError.UNAUTHORIZED
        is FirebaseNetworkException -> ContactError.NETWORK_UNAVAILABLE
        is FirebaseFirestoreException -> when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> ContactError.UNAUTHORIZED
            FirebaseFirestoreException.Code.NOT_FOUND -> ContactError.CONTACT_NOT_FOUND
            FirebaseFirestoreException.Code.UNAVAILABLE -> ContactError.NETWORK_UNAVAILABLE
            else -> ContactError.UNKNOWN
        }
        else -> ContactError.UNKNOWN
    }

    private data object ContactNotFoundException : IllegalStateException()
    private data object ContactUnauthorizedException : IllegalStateException()
}
