package com.brandcrafts.erp.feature.inventory

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.ui.components.EmptyState
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import com.brandcrafts.erp.ui.components.SearchBar
import com.brandcrafts.erp.ui.components.StatusChip
import com.brandcrafts.erp.ui.components.StatusTone
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import java.text.NumberFormat

@Composable
fun InventoryScreen(
    uiState: InventoryUiState,
    isAdmin: Boolean,
    onEvent: (InventoryUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when (uiState) {
        is InventoryUiState.Loading -> LoadingView(
            message = stringResource(R.string.inventory_loading),
            modifier = modifier,
        )
        is InventoryUiState.Error -> InventoryErrorContent(
            type = uiState.type,
            onRetryClick = { onEvent(InventoryUiEvent.RetryClicked) },
            modifier = modifier,
        )
        is InventoryUiState.Content -> InventoryListContent(
            query = uiState.searchQuery,
            items = uiState.items,
            isAdmin = isAdmin,
            onEvent = onEvent,
            modifier = modifier,
        )
        is InventoryUiState.Empty -> InventoryEmptyContent(
            query = uiState.searchQuery,
            isAdmin = isAdmin,
            onEvent = onEvent,
            modifier = modifier,
        )
    }
}

@Composable
private fun InventoryListContent(
    query: String,
    items: List<InventoryListItem>,
    isAdmin: Boolean,
    onEvent: (InventoryUiEvent) -> Unit,
    modifier: Modifier,
) {
    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            InventorySearchRow(query = query, onEvent = onEvent)
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 24.dp, top = 8.dp, end = 24.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items = items, key = InventoryListItem::id) { item ->
                    InventoryItemCard(
                        item = item,
                        showFinancialFields = isAdmin,
                    onClick = { onEvent(InventoryUiEvent.ItemClicked(item.id)) },
                    onStockInClick = { onEvent(InventoryUiEvent.StockInClicked(item.id)) },
                        onEditClick = if (isAdmin) {
                            { onEvent(InventoryUiEvent.EditItemClicked(item.id)) }
                        } else {
                            null
                        },
                    )
                }
            }
        }
        if (isAdmin) {
            InventoryAddItemButton(onClick = { onEvent(InventoryUiEvent.AddItemClicked) })
        }
    }
}

@Composable
private fun InventoryEmptyContent(
    query: String,
    isAdmin: Boolean,
    onEvent: (InventoryUiEvent) -> Unit,
    modifier: Modifier,
) {
    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            InventorySearchRow(query = query, onEvent = onEvent)
            EmptyState(
                title = stringResource(
                    if (query.isBlank()) R.string.inventory_empty_title else R.string.inventory_no_search_results_title,
                ),
                description = stringResource(
                    if (query.isBlank()) R.string.inventory_empty_description else R.string.inventory_no_search_results_description,
                ),
                icon = Icons.Outlined.Inventory2,
                actionLabel = if (isAdmin && query.isBlank()) stringResource(R.string.inventory_add_material) else null,
                onActionClick = if (isAdmin && query.isBlank()) {
                    { onEvent(InventoryUiEvent.AddItemClicked) }
                } else {
                    null
                },
                modifier = Modifier.weight(1f),
            )
        }
        if (isAdmin) {
            InventoryAddItemButton(onClick = { onEvent(InventoryUiEvent.AddItemClicked) })
        }
    }
}

@Composable
private fun InventorySearchRow(
    query: String,
    onEvent: (InventoryUiEvent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 24.dp, top = 16.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SearchBar(
            query = query,
            onQueryChange = { onEvent(InventoryUiEvent.SearchQueryChanged(it)) },
            placeholder = stringResource(R.string.inventory_search_placeholder),
            searchIcon = Icons.Outlined.Search,
            searchIconContentDescription = stringResource(R.string.inventory_search_icon_description),
            modifier = Modifier.weight(1f),
        )
        IconButton(
            onClick = { onEvent(InventoryUiEvent.FilterClicked) },
        ) {
            Icon(
                imageVector = Icons.Outlined.FilterList,
                contentDescription = stringResource(R.string.inventory_filter_action),
            )
        }
    }
}

@Composable
private fun InventoryErrorContent(
    type: InventoryErrorType,
    onRetryClick: () -> Unit,
    modifier: Modifier,
) {
    ErrorState(
        title = stringResource(R.string.inventory_error_title),
        description = stringResource(
            when (type) {
                InventoryErrorType.NETWORK -> R.string.inventory_error_network
                InventoryErrorType.UNAUTHORIZED -> R.string.inventory_error_unauthorized
                InventoryErrorType.UNKNOWN -> R.string.inventory_error_unknown
            },
        ),
        retryLabel = stringResource(R.string.retry),
        onRetry = onRetryClick,
        modifier = modifier,
    )
}

@Composable
private fun InventoryItemCard(
    item: InventoryListItem,
    showFinancialFields: Boolean,
    onClick: () -> Unit,
    onStockInClick: () -> Unit,
    onEditClick: (() -> Unit)?,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        ListItem(
            headlineContent = { Text(text = item.name, style = MaterialTheme.typography.titleMedium) },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = stringResource(R.string.inventory_item_metadata, item.sku, item.category),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    Text(
                        text = stringResource(
                            R.string.inventory_available_quantity,
                            item.availableQuantity.asNumberText(),
                            item.unit,
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (showFinancialFields) {
                        Text(
                            text = stringResource(
                                R.string.inventory_financial_rates,
                                item.purchasePrice.asNumberText(),
                                item.sellingPrice.asNumberText(),
                            ),
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            trailingContent = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusChip(
                        label = stringResource(
                            if (item.isLowStock) R.string.inventory_status_low_stock else R.string.inventory_status_in_stock,
                        ),
                        tone = if (item.isLowStock) StatusTone.WARNING else StatusTone.SUCCESS,
                    )
                    if (!item.active) {
                        StatusChip(
                            label = stringResource(R.string.inventory_status_inactive),
                            tone = StatusTone.NEUTRAL,
                        )
                    }
                    androidx.compose.material3.TextButton(onClick = onStockInClick) {
                        Text(stringResource(R.string.stock_in_title))
                    }
                    onEditClick?.let { action ->
                        IconButton(onClick = action) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = stringResource(R.string.inventory_edit_action),
                            )
                        }
                    }
                }
            },
        )
    }
}

@Composable
private fun InventoryAddItemButton(onClick: () -> Unit) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        contentAlignment = Alignment.BottomEnd,
    ) {
        ExtendedFloatingActionButton(
            text = { Text(stringResource(R.string.inventory_add_material)) },
            icon = { Icon(Icons.Outlined.Add, contentDescription = null) },
            onClick = onClick,
        )
    }
}

private fun Double.asNumberText(): String = NumberFormat.getNumberInstance().format(this)

@Preview(showBackground = true)
@Composable
private fun InventoryAdminPreview() {
    BrandCraftsTheme {
        InventoryScreen(
            uiState = InventoryUiState.Content("", previewInventoryItems()),
            isAdmin = true,
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InventoryEmployeeDarkPreview() {
    BrandCraftsTheme(darkTheme = true) {
        InventoryScreen(
            uiState = InventoryUiState.Content("", previewInventoryItems()),
            isAdmin = false,
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InventoryEmptyPreview() {
    BrandCraftsTheme { InventoryScreen(InventoryUiState.Empty(""), true, onEvent = {}) }
}

@Preview(showBackground = true)
@Composable
private fun InventoryLoadingPreview() {
    BrandCraftsTheme { InventoryScreen(InventoryUiState.Loading(), true, onEvent = {}) }
}

@Preview(showBackground = true)
@Composable
private fun InventoryErrorPreview() {
    BrandCraftsTheme { InventoryScreen(InventoryUiState.Error("", InventoryErrorType.NETWORK), true, onEvent = {}) }
}

private fun previewInventoryItems() = listOf(
    InventoryListItem("material-1", "Blue Vinyl", "VIN-BLU-01", "Vinyl", "rolls", 2.0, 5.0, 140.0, 220.0, true),
    InventoryListItem("material-2", "White Flex", "FLX-WHT-01", "Flex", "meters", 24.0, 6.0, 75.0, 120.0, true),
)
