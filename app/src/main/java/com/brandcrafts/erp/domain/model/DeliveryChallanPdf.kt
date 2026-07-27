package com.brandcrafts.erp.domain.model

data class DeliveryChallanPdf(val cacheFileName: String)

sealed interface DeliveryChallanPdfError {
    data object CompanyConfigurationIncomplete : DeliveryChallanPdfError
    data object CustomerUnavailable : DeliveryChallanPdfError
    data object DeliveryChallanUnavailable : DeliveryChallanPdfError
    data object GenerationFailed : DeliveryChallanPdfError
}

class DeliveryChallanPdfFailure(val error: DeliveryChallanPdfError) : RuntimeException()
