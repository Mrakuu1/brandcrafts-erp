package com.brandcrafts.erp.domain.model

data class PurchaseOrderPdf(val cacheFileName: String)

sealed interface PurchaseOrderPdfError {
    data object CompanyConfigurationIncomplete : PurchaseOrderPdfError
    data object SupplierUnavailable : PurchaseOrderPdfError
    data object PurchaseOrderUnavailable : PurchaseOrderPdfError
    data object GenerationFailed : PurchaseOrderPdfError
}

class PurchaseOrderPdfFailure(val error: PurchaseOrderPdfError) : RuntimeException()
