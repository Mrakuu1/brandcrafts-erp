package com.brandcrafts.erp.domain.model

data class InvoicePdf(val cacheFileName: String)

sealed interface InvoicePdfError {
    data object CompanyConfigurationIncomplete : InvoicePdfError
    data object CustomerUnavailable : InvoicePdfError
    data object InvoiceUnavailable : InvoicePdfError
    data object GenerationFailed : InvoicePdfError
}

class InvoicePdfFailure(val error: InvoicePdfError) : RuntimeException()
