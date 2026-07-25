package com.brandcrafts.erp.domain.model
data class QuotationPdf(val cacheFileName:String)
sealed interface QuotationPdfError { data object CompanyConfigurationIncomplete:QuotationPdfError; data object CustomerUnavailable:QuotationPdfError; data object GenerationFailed:QuotationPdfError; data object SharingUnavailable:QuotationPdfError }
