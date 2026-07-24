package com.brandcrafts.erp.feature.inventory

import androidx.annotation.StringRes

enum class InventoryFormMode {
    CREATE,
    EDIT,
}

data class InventoryFormUiState(
    val mode: InventoryFormMode,
    val itemId: String? = null,
    val name: String = "",
    val sku: String = "",
    val category: String = "",
    val unit: String = "",
    val availableQuantity: String = "0",
    val minimumQuantity: String = "0",
    val purchasePrice: String = "0",
    val sellingPrice: String = "0",
    val description: String = "",
    val active: Boolean = true,
    val errors: InventoryFormFieldErrors = InventoryFormFieldErrors(),
    val isInitialLoading: Boolean = mode == InventoryFormMode.EDIT,
    val isSaving: Boolean = false,
    val loadError: InventoryFormErrorType? = null,
    val saveError: InventoryFormErrorType? = null,
)

data class InventoryFormFieldErrors(
    @param:StringRes val name: Int? = null,
    @param:StringRes val sku: Int? = null,
    @param:StringRes val category: Int? = null,
    @param:StringRes val unit: Int? = null,
    @param:StringRes val availableQuantity: Int? = null,
    @param:StringRes val minimumQuantity: Int? = null,
    @param:StringRes val purchasePrice: Int? = null,
    @param:StringRes val sellingPrice: Int? = null,
)

enum class InventoryFormErrorType {
    NETWORK,
    UNAUTHORIZED,
    DUPLICATE_SKU,
    NOT_FOUND,
    UNKNOWN,
}
