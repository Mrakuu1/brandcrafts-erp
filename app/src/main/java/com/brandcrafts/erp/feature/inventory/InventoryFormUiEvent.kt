package com.brandcrafts.erp.feature.inventory

sealed interface InventoryFormUiEvent {
    data class NameChanged(val value: String) : InventoryFormUiEvent
    data class SkuChanged(val value: String) : InventoryFormUiEvent
    data class CategoryChanged(val value: String) : InventoryFormUiEvent
    data class UnitChanged(val value: String) : InventoryFormUiEvent
    data class AvailableQuantityChanged(val value: String) : InventoryFormUiEvent
    data class MinimumQuantityChanged(val value: String) : InventoryFormUiEvent
    data class PurchasePriceChanged(val value: String) : InventoryFormUiEvent
    data class SellingPriceChanged(val value: String) : InventoryFormUiEvent
    data class DescriptionChanged(val value: String) : InventoryFormUiEvent
    data class ActiveChanged(val value: Boolean) : InventoryFormUiEvent
    data object SaveClicked : InventoryFormUiEvent
    data object CancelClicked : InventoryFormUiEvent
    data object RetryLoadClicked : InventoryFormUiEvent
}
