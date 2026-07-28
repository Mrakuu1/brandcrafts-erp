package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.core.result.ContactResult
import com.brandcrafts.erp.data.pdf.QuotationPdfRenderer
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.model.QuotationPdf
import com.brandcrafts.erp.domain.model.QuotationPdfError
import com.brandcrafts.erp.domain.model.QuotationPdfFailure
import com.brandcrafts.erp.domain.repository.CompanyConfigRepository
import com.brandcrafts.erp.domain.repository.ContactRepository
import com.brandcrafts.erp.domain.repository.QuotationPdfRepository
import com.brandcrafts.erp.domain.repository.QuotationRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class QuotationPdfRepositoryImpl @Inject constructor(
    private val quotations: QuotationRepository,
    private val contacts: ContactRepository,
    private val companyConfig: CompanyConfigRepository,
    private val renderer: QuotationPdfRenderer,
) : QuotationPdfRepository {
    override suspend fun generate(quotationId: String): Result<QuotationPdf> {
        return try {
        val quotation = quotations.getQuotation(quotationId).getOrElse {
            return Result.failure(QuotationPdfFailure(QuotationPdfError.GenerationFailed))
        }
        val customer = (contacts.getContact(quotation.contactId) as? ContactResult.Success)
            ?.data
            ?.takeIf { it.type == ContactType.CUSTOMER }
            ?: return Result.failure(QuotationPdfFailure(QuotationPdfError.CustomerUnavailable))
        val company = companyConfig.getCompanyConfig().getOrElse {
            return Result.failure(QuotationPdfFailure(QuotationPdfError.CompanyConfigurationIncomplete))
        }
        if (company.companyName.isBlank() || company.addressLine1.isBlank() || company.phone.isBlank() || company.email.isBlank()) {
            return Result.failure(QuotationPdfFailure(QuotationPdfError.CompanyConfigurationIncomplete))
        }
        renderer.render(quotation, customer, company).fold(
            onSuccess = { Result.success(QuotationPdf(it.name)) },
            onFailure = { exception ->
                Result.failure(QuotationPdfFailure(QuotationPdfError.GenerationFailed, exception))
            },
        )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Throwable) {
            Result.failure(QuotationPdfFailure(QuotationPdfError.GenerationFailed, exception))
        }
    }
}
