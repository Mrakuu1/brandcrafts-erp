package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.core.result.ContactResult
import com.brandcrafts.erp.data.pdf.PurchaseOrderPdfRenderer
import com.brandcrafts.erp.domain.model.ContactType
import com.brandcrafts.erp.domain.model.PurchaseOrderPdf
import com.brandcrafts.erp.domain.model.PurchaseOrderPdfError
import com.brandcrafts.erp.domain.model.PurchaseOrderPdfFailure
import com.brandcrafts.erp.domain.repository.CompanyConfigRepository
import com.brandcrafts.erp.domain.repository.ContactRepository
import com.brandcrafts.erp.domain.repository.PurchaseOrderPdfRepository
import com.brandcrafts.erp.domain.repository.PurchaseOrderRepository
import javax.inject.Inject

class PurchaseOrderPdfRepositoryImpl @Inject constructor(
    private val purchaseOrders: PurchaseOrderRepository,
    private val contacts: ContactRepository,
    private val companyConfig: CompanyConfigRepository,
    private val renderer: PurchaseOrderPdfRenderer,
) : PurchaseOrderPdfRepository {
    override suspend fun generate(purchaseOrderId: String): Result<PurchaseOrderPdf> = try {
        val order = purchaseOrders.getPurchaseOrder(purchaseOrderId).getOrElse { throw PurchaseOrderPdfFailure(PurchaseOrderPdfError.PurchaseOrderUnavailable) }
        val supplier = when (val result = contacts.getContact(order.supplierId)) {
            is ContactResult.Success -> result.data.takeIf { it.type == ContactType.SUPPLIER }
            is ContactResult.Error -> null
        } ?: throw PurchaseOrderPdfFailure(PurchaseOrderPdfError.SupplierUnavailable)
        val company = companyConfig.getCompanyConfig().getOrElse { throw PurchaseOrderPdfFailure(PurchaseOrderPdfError.CompanyConfigurationIncomplete) }
        renderer.render(order, supplier, company).fold(
            onSuccess = { Result.success(PurchaseOrderPdf(it.name)) },
            onFailure = { Result.failure(PurchaseOrderPdfFailure(PurchaseOrderPdfError.GenerationFailed)) },
        )
    } catch (failure: PurchaseOrderPdfFailure) {
        Result.failure(failure)
    } catch (exception: kotlinx.coroutines.CancellationException) {
        throw exception
    } catch (exception: Throwable) {
        Result.failure(PurchaseOrderPdfFailure(PurchaseOrderPdfError.GenerationFailed))
    }
}
