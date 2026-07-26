package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.core.result.ContactError
import com.brandcrafts.erp.core.result.ContactResult
import com.brandcrafts.erp.core.result.InvoiceError
import com.brandcrafts.erp.core.result.InvoiceFailure
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.repository.ContactRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

data class InvoiceValidatedCustomer(
    val customerId: String,
    val name: String,
    val businessName: String,
    val phone: String,
    val gstNumber: String,
)

class InvoiceCustomerValidator @Inject constructor(
    private val contacts: ContactRepository,
) {
    suspend fun requireActiveCustomer(customerId: String): InvoiceValidatedCustomer {
        if (customerId.isBlank()) throw InvoiceFailure(InvoiceError.CustomerRequired)
        return try {
            when (val result = contacts.getContact(customerId)) {
                is ContactResult.Success -> result.data.let { contact ->
                    if (contact.type != ContactType.CUSTOMER) throw InvoiceFailure(InvoiceError.ContactIsNotCustomer)
                    if (!contact.active) throw InvoiceFailure(InvoiceError.CustomerInactive)
                    InvoiceValidatedCustomer(contact.id, contact.name, contact.company, contact.phone, contact.gstNumber)
                }
                is ContactResult.Error -> throw InvoiceFailure(result.error.toInvoiceError())
            }
        } catch (exception: CancellationException) {
            throw exception
        }
    }
}

private fun ContactError.toInvoiceError(): InvoiceError = when (this) {
    ContactError.CONTACT_NOT_FOUND -> InvoiceError.CustomerNotFound
    ContactError.UNAUTHORIZED -> InvoiceError.PermissionDenied
    ContactError.NETWORK_UNAVAILABLE -> InvoiceError.FirestoreUnavailable
    else -> InvoiceError.RepositoryUnavailable
}
