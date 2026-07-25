package com.brandcrafts.erp.feature.quotation
sealed interface QuotationFormUiEffect{data class Saved(val id:String):QuotationFormUiEffect;data object NavigateBack:QuotationFormUiEffect;data object Unauthorized:QuotationFormUiEffect;data object EditingBlocked:QuotationFormUiEffect}
