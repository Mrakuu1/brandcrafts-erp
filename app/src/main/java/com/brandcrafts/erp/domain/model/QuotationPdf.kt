package com.brandcrafts.erp.domain.model

data class QuotationPdf(val cacheFileName: String)

sealed interface QuotationPdfError {
    data object CompanyConfigurationIncomplete : QuotationPdfError
    data object CustomerUnavailable : QuotationPdfError
    data object GenerationFailed : QuotationPdfError
    data object SharingUnavailable : QuotationPdfError
}

/** A presentation-safe typed failure for quotation PDF operations. */
class QuotationPdfFailure(
    val error: QuotationPdfError,
    cause: Throwable? = null,
) : Exception(null, cause)
