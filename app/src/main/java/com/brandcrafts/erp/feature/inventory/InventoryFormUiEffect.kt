package com.brandcrafts.erp.feature.inventory

sealed interface InventoryFormUiEffect {
    data object NavigateBack : InventoryFormUiEffect
    data object ItemSaved : InventoryFormUiEffect
}
