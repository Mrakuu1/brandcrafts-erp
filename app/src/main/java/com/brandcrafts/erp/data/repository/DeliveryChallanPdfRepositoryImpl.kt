package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.core.result.ContactResult
import com.brandcrafts.erp.data.pdf.DeliveryChallanPdfRenderer
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.model.DeliveryChallanPdf
import com.brandcrafts.erp.domain.model.DeliveryChallanPdfError
import com.brandcrafts.erp.domain.model.DeliveryChallanPdfFailure
import com.brandcrafts.erp.domain.repository.CompanyConfigRepository
import com.brandcrafts.erp.domain.repository.ContactRepository
import com.brandcrafts.erp.domain.repository.DeliveryChallanPdfRepository
import com.brandcrafts.erp.domain.repository.DeliveryChallanRepository
import javax.inject.Inject
import kotlinx.coroutines.CancellationException

class DeliveryChallanPdfRepositoryImpl @Inject constructor(
    private val deliveryChallans: DeliveryChallanRepository,
    private val contacts: ContactRepository,
    private val companyConfig: CompanyConfigRepository,
    private val renderer: DeliveryChallanPdfRenderer,
) : DeliveryChallanPdfRepository {
    override suspend fun generate(challanId: String): Result<DeliveryChallanPdf> = try {
        val challan = deliveryChallans.getDeliveryChallan(challanId).getOrElse {
            throw DeliveryChallanPdfFailure(DeliveryChallanPdfError.DeliveryChallanUnavailable)
        }
        val customer = when (val result = contacts.getContact(challan.customerId)) {
            is ContactResult.Success -> result.data.takeIf { it.type == ContactType.CUSTOMER }
            is ContactResult.Error -> null
        } ?: throw DeliveryChallanPdfFailure(DeliveryChallanPdfError.CustomerUnavailable)
        val company = companyConfig.getCompanyConfig().getOrElse {
            throw DeliveryChallanPdfFailure(DeliveryChallanPdfError.CompanyConfigurationIncomplete)
        }
        renderer.render(challan, customer, company).fold(
            onSuccess = { Result.success(DeliveryChallanPdf(it.name)) },
            onFailure = { Result.failure(DeliveryChallanPdfFailure(DeliveryChallanPdfError.GenerationFailed)) },
        )
    } catch (exception: CancellationException) {
        throw exception
    } catch (failure: DeliveryChallanPdfFailure) {
        Result.failure(failure)
    } catch (_: Throwable) {
        Result.failure(DeliveryChallanPdfFailure(DeliveryChallanPdfError.GenerationFailed))
    }
}
