package com.brandcrafts.erp.feature.inventory

sealed interface InventoryUiState {
    val searchQuery: String

    data class Loading(
        override val searchQuery: String = "",
    ) : InventoryUiState

    data class Content(
        override val searchQuery: String,
        val items: List<InventoryListItem>,
    ) : InventoryUiState

    data class Empty(
        override val searchQuery: String,
    ) : InventoryUiState

    data class Error(
        override val searchQuery: String,
        val type: InventoryErrorType,
    ) : InventoryUiState
}

enum class InventoryErrorType {
    NETWORK,
    UNAUTHORIZED,
    UNKNOWN,
}
