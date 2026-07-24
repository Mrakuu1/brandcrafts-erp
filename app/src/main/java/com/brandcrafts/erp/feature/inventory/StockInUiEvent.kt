package com.brandcrafts.erp.feature.inventory

sealed interface StockInUiEvent {
    data class QuantityChanged(val value: String) : StockInUiEvent
    data class ReferenceChanged(val value: String) : StockInUiEvent
    data class RemarksChanged(val value: String) : StockInUiEvent
    data object SaveClicked : StockInUiEvent
    data object CancelClicked : StockInUiEvent
    data object RetryClicked : StockInUiEvent
}
