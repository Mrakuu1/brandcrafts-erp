package com.brandcrafts.erp.feature.inventory

sealed interface InventoryUiState {
    val searchQuery: String
    val filter: InventoryFilter

    data class Loading(
        override val searchQuery: String = "",
        override val filter: InventoryFilter = InventoryFilter.ALL,
    ) : InventoryUiState

    data class Content(
        override val searchQuery: String,
        val items: List<InventoryListItem>,
        override val filter: InventoryFilter = InventoryFilter.ALL,
    ) : InventoryUiState

    data class Empty(
        override val searchQuery: String,
        override val filter: InventoryFilter = InventoryFilter.ALL,
    ) : InventoryUiState

    data class Error(
        override val searchQuery: String,
        val type: InventoryErrorType,
        override val filter: InventoryFilter = InventoryFilter.ALL,
    ) : InventoryUiState
}

enum class InventoryFilter {
    ALL,
    LOW_STOCK,
    ACTIVE,
    INACTIVE,
}

enum class InventoryErrorType {
    NETWORK,
    UNAUTHORIZED,
    UNKNOWN,
}
