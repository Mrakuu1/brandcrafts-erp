package com.brandcrafts.erp.feature.inventory

import androidx.annotation.StringRes

sealed interface InventoryUiEffect {
    data class NavigateToItemDetails(val itemId: String) : InventoryUiEffect
    data class NavigateToEditItem(val itemId: String) : InventoryUiEffect
    data class NavigateToStockIn(val itemId: String) : InventoryUiEffect
    data class NavigateToStockOut(val itemId: String) : InventoryUiEffect
    data object NavigateToCreateItem : InventoryUiEffect
    data class ShowMessage(@param:StringRes val messageRes: Int) : InventoryUiEffect
}
