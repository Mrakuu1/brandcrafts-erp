package com.brandcrafts.erp.feature.inventory

sealed interface InventoryUiEvent {
    data class SearchQueryChanged(val query: String) : InventoryUiEvent
    data class ItemClicked(val itemId: String) : InventoryUiEvent
    data class EditItemClicked(val itemId: String) : InventoryUiEvent
    data class StockInClicked(val itemId: String) : InventoryUiEvent
    data class StockOutClicked(val itemId: String) : InventoryUiEvent
    data object AddItemClicked : InventoryUiEvent
    data class FilterChanged(val filter: InventoryFilter) : InventoryUiEvent
    data object RetryClicked : InventoryUiEvent
}
