package com.brandcrafts.erp.feature.quotation

import com.brandcrafts.erp.domain.model.Quotation
import com.brandcrafts.erp.domain.model.QuotationStatus

data class QuotationUiState(
    val content: Content = Content.Loading,
    val query: String = "",
    val status: QuotationStatus? = null,
    val all: List<Quotation> = emptyList(),
    val visible: List<QuotationListItem> = emptyList(),
) {
    sealed interface Content {
        data object Loading : Content
        data object Loaded : Content
        data object Empty : Content
        data object Error : Content
    }
}

data class QuotationListItem(
    val quotation: Quotation,
    val customerDisplayName: String?,
)
