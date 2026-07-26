package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.core.result.ContactResult
import com.brandcrafts.erp.data.pdf.InvoicePdfRenderer
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.model.InvoicePdf
import com.brandcrafts.erp.domain.model.InvoicePdfError
import com.brandcrafts.erp.domain.model.InvoicePdfFailure
import com.brandcrafts.erp.domain.repository.CompanyConfigRepository
import com.brandcrafts.erp.domain.repository.ContactRepository
import com.brandcrafts.erp.domain.repository.InvoicePdfRepository
import com.brandcrafts.erp.domain.repository.InvoiceRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class InvoicePdfRepositoryImpl @Inject constructor(
    private val invoices: InvoiceRepository,
    private val contacts: ContactRepository,
    private val companyConfig: CompanyConfigRepository,
    private val renderer: InvoicePdfRenderer,
) : InvoicePdfRepository {
    override suspend fun generate(invoiceId: String): Result<InvoicePdf> = try {
        val invoice = invoices.getInvoice(invoiceId).getOrElse {
            throw InvoicePdfFailure(InvoicePdfError.InvoiceUnavailable)
        }
        val customer = when (val result = contacts.getContact(invoice.customerId)) {
            is ContactResult.Success -> result.data.takeIf { it.type == ContactType.CUSTOMER }
            is ContactResult.Error -> null
        } ?: throw InvoicePdfFailure(InvoicePdfError.CustomerUnavailable)
        val company = companyConfig.getCompanyConfig().getOrElse {
            throw InvoicePdfFailure(InvoicePdfError.CompanyConfigurationIncomplete)
        }
        if (!company.hasRequiredPdfIdentity()) {
            throw InvoicePdfFailure(InvoicePdfError.CompanyConfigurationIncomplete)
        }
        renderer.render(invoice, customer, company).fold(
            onSuccess = { Result.success(InvoicePdf(it.name)) },
            onFailure = { Result.failure(InvoicePdfFailure(InvoicePdfError.GenerationFailed)) },
        )
    } catch (exception: CancellationException) {
        throw exception
    } catch (failure: InvoicePdfFailure) {
        Result.failure(failure)
    } catch (exception: Throwable) {
        Result.failure(InvoicePdfFailure(InvoicePdfError.GenerationFailed))
    }

    private fun com.brandcrafts.erp.domain.model.CompanyConfig.hasRequiredPdfIdentity(): Boolean =
        companyName.isNotBlank() &&
            addressLine1.isNotBlank() &&
            city.isNotBlank() &&
            state.isNotBlank() &&
            pincode.isNotBlank() &&
            country.isNotBlank() &&
            phone.isNotBlank() &&
            email.isNotBlank()
}
