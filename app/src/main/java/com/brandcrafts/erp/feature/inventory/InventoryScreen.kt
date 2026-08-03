package com.brandcrafts.erp.feature.inventory

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sell
import androidx.compose.material.icons.outlined.SouthEast
import androidx.compose.material.icons.outlined.NorthEast
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.ui.bottomsheet.BrandBottomSheet
import com.brandcrafts.erp.ui.LocalBottomChromeVisible
import com.brandcrafts.erp.ui.components.EmptyState
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import com.brandcrafts.erp.ui.theme.BrandMotion
import java.text.NumberFormat

@Composable
fun InventoryScreen(
    uiState: InventoryUiState,
    isAdmin: Boolean,
    onEvent: (InventoryUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    var filtersOpen by remember { mutableStateOf(false) }
    when (uiState) {
        is InventoryUiState.Loading -> LoadingView(modifier = modifier, message = stringResource(R.string.inventory_loading))
        is InventoryUiState.Error -> InventoryErrorContent(uiState.type, { onEvent(InventoryUiEvent.RetryClicked) }, modifier)
        is InventoryUiState.Content -> InventoryContent(
            query = uiState.searchQuery,
            filter = uiState.filter,
            items = uiState.items,
            isAdmin = isAdmin,
            onEvent = onEvent,
            onOpenFilters = { filtersOpen = true },
            modifier = modifier,
        )
        is InventoryUiState.Empty -> InventoryEmptyContent(
            query = uiState.searchQuery,
            filter = uiState.filter,
            isAdmin = isAdmin,
            onEvent = onEvent,
            onOpenFilters = { filtersOpen = true },
            modifier = modifier,
        )
    }
    if (filtersOpen) {
        InventoryFilterBottomSheet(
            selectedFilter = uiState.filter,
            onApply = { onEvent(InventoryUiEvent.FilterChanged(it)); filtersOpen = false },
            onDismissRequest = { filtersOpen = false },
        )
    }
}

@Composable
private fun InventoryContent(
    query: String,
    filter: InventoryFilter,
    items: List<InventoryListItem>,
    isAdmin: Boolean,
    onEvent: (InventoryUiEvent) -> Unit,
    onOpenFilters: () -> Unit,
    modifier: Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(inventoryPageColor())) {
        Column(Modifier.fillMaxSize()) {
            InventorySearchRow(query, filter != InventoryFilter.ALL, onEvent, onOpenFilters)
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 94.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(items, key = InventoryListItem::id) { item ->
                    InventoryItemCard(
                        item = item,
                        showFinancialFields = isAdmin,
                        onClick = { onEvent(InventoryUiEvent.ItemClicked(item.id)) },
                        onStockInClick = { onEvent(InventoryUiEvent.StockInClicked(item.id)) },
                        onStockOutClick = { onEvent(InventoryUiEvent.StockOutClicked(item.id)) },
                        onEditClick = if (isAdmin) ({ onEvent(InventoryUiEvent.EditItemClicked(item.id)) }) else null,
                    )
                }
            }
        }
        if (isAdmin) InventoryAddItemButton { onEvent(InventoryUiEvent.AddItemClicked) }
    }
}

@Composable
private fun InventoryEmptyContent(
    query: String,
    filter: InventoryFilter,
    isAdmin: Boolean,
    onEvent: (InventoryUiEvent) -> Unit,
    onOpenFilters: () -> Unit,
    modifier: Modifier,
) {
    Box(modifier = modifier.fillMaxSize().background(inventoryPageColor())) {
        Column(Modifier.fillMaxSize()) {
            InventorySearchRow(query, filter != InventoryFilter.ALL, onEvent, onOpenFilters)
            EmptyState(
                title = stringResource(if (query.isBlank()) R.string.inventory_empty_title else R.string.inventory_no_search_results_title),
                description = stringResource(if (query.isBlank()) R.string.inventory_empty_description else R.string.inventory_no_search_results_description),
                icon = Icons.Outlined.Inventory2,
                actionLabel = if (isAdmin && query.isBlank()) stringResource(R.string.inventory_add_material) else null,
                onActionClick = if (isAdmin && query.isBlank()) ({ onEvent(InventoryUiEvent.AddItemClicked) }) else null,
                modifier = Modifier.weight(1f),
            )
        }
        if (isAdmin) InventoryAddItemButton { onEvent(InventoryUiEvent.AddItemClicked) }
    }
}

@Composable
private fun InventorySearchRow(query: String, filterActive: Boolean, onEvent: (InventoryUiEvent) -> Unit, onOpenFilters: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, end = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        InventorySearchField(query, { onEvent(InventoryUiEvent.SearchQueryChanged(it)) }, Modifier.weight(1f))
        Box(
            modifier = Modifier
                .size(56.dp)
                .shadow(3.dp, CircleShape, clip = false)
                .clip(CircleShape)
                .background(if (inventoryDark()) Color(0xFF16212E) else Color.White)
                .border(1.dp, if (filterActive) MaterialTheme.colorScheme.primary else inventoryOutline(), CircleShape)
                .clickable(onClick = onOpenFilters),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.FilterList, stringResource(R.string.inventory_filter_action), tint = if (filterActive) MaterialTheme.colorScheme.primary else inventorySecondary())
        }
    }
}

@Composable
private fun InventorySearchField(query: String, onQueryChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(22.dp)
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.height(56.dp).shadow(if (inventoryDark()) 0.dp else 3.dp, shape, clip = false).clip(shape),
        shape = shape,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyMedium,
        placeholder = {
            Text(
                stringResource(R.string.inventory_search_placeholder),
                style = MaterialTheme.typography.bodyMedium,
                color = inventorySecondary(),
            )
        },
        leadingIcon = { Icon(Icons.Outlined.Search, stringResource(R.string.inventory_search_icon_description), tint = inventorySecondary(), modifier = Modifier.size(19.dp)) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = if (inventoryDark()) Color(0xFF16212E) else Color.White,
            unfocusedContainerColor = if (inventoryDark()) Color(0xFF16212E) else Color.White,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
        ),
    )
}

@Composable
private fun InventoryFilterBottomSheet(selectedFilter: InventoryFilter, onApply: (InventoryFilter) -> Unit, onDismissRequest: () -> Unit) {
    var pendingFilter by remember(selectedFilter) { mutableStateOf(selectedFilter) }
    BrandBottomSheet(
        title = stringResource(R.string.inventory_apply_filters),
        onDismissRequest = onDismissRequest,
        containerColor = if (inventoryDark()) Color(0xFF111A25) else Color.White,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(420.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, inventoryOutline(), RoundedCornerShape(12.dp))
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                InventoryFilter.entries.forEach { filter ->
                    val selected = pendingFilter == filter
                    Text(
                        text = stringResource(filter.labelRes()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selected) if (inventoryDark()) Color(0xFF202E3D) else Color(0xFFF7F7F7) else Color.Transparent)
                            .clickable { pendingFilter = filter }
                            .padding(vertical = 18.dp, horizontal = 16.dp),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal),
                        color = if (selected) MaterialTheme.colorScheme.onSurface else inventorySecondary(),
                        textAlign = TextAlign.Center,
                    )
                }
            }
            InventoryGradientButton(
                text = stringResource(R.string.inventory_apply_filters),
                onClick = { onApply(pendingFilter) },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun InventoryItemCard(
    item: InventoryListItem,
    showFinancialFields: Boolean,
    onClick: () -> Unit,
    onStockInClick: () -> Unit,
    onStockOutClick: () -> Unit,
    onEditClick: (() -> Unit)?,
) {
    val shape = RoundedCornerShape(12.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(inventoryCardColor())
            .border(1.dp, inventoryOutline(), shape)
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFFFF6500).copy(alpha = if (inventoryDark()) .20f else .12f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Inventory2, null, tint = Color(0xFFFF6500), modifier = Modifier.size(25.dp)) }
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(item.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(stringResource(R.string.inventory_item_metadata, item.sku, item.category), style = MaterialTheme.typography.labelSmall, color = inventorySecondary(), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            InventoryStockStatus(item)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Inventory2, null, tint = inventorySecondary(), modifier = Modifier.size(15.dp))
            Text(
                text = stringResource(R.string.inventory_available_quantity, item.availableQuantity.asNumberText(), item.unit),
                modifier = Modifier.padding(start = 5.dp).weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Sell, null, tint = inventorySecondary(), modifier = Modifier.size(15.dp))
            if (showFinancialFields) {
                Text(
                    text = stringResource(R.string.inventory_financial_rates, item.purchasePrice.asNumberText(), item.sellingPrice.asNumberText()),
                    modifier = Modifier.padding(start = 5.dp).weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = inventorySecondary(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            } else Box(modifier = Modifier.weight(1f))
        }
        HorizontalDivider(color = inventoryOutline())
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            InventoryCardAction(stringResource(R.string.stock_in_title), Icons.Outlined.NorthEast, onStockInClick, Modifier.weight(1f))
            InventoryCardAction(stringResource(R.string.stock_out_title), Icons.Outlined.SouthEast, onStockOutClick, Modifier.weight(1f))
            onEditClick?.let { InventoryCardAction(stringResource(R.string.inventory_edit_action), Icons.Outlined.Edit, it, Modifier.weight(1f)) }
        }
    }
}

@Composable
private fun InventoryCardAction(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick).padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
        Icon(icon, null, modifier = Modifier.padding(start = 3.dp).size(15.dp), tint = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun InventoryStockStatus(item: InventoryListItem) {
    val (label, dot, background) = when {
        item.availableQuantity <= 0.0 -> Triple(R.string.inventory_status_out_of_stock, Color(0xFFE53935), Color(0xFFFFE8E7))
        item.isLowStock -> Triple(R.string.inventory_status_low_stock, Color(0xFFFFA000), Color(0xFFFFF4D7))
        else -> Triple(R.string.inventory_status_in_stock, Color(0xFF159447), Color(0xFFE5F6EB))
    }
    Row(
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(if (inventoryDark()) dot.copy(alpha = .17f) else background).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(6.dp).background(dot, CircleShape))
        Text(stringResource(label), modifier = Modifier.padding(start = 5.dp), style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold), color = dot)
    }
}

@Composable
private fun InventoryAddItemButton(onClick: () -> Unit) {
    AnimatedVisibility(
        visible = LocalBottomChromeVisible.current,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
    Box(modifier = Modifier.fillMaxSize().padding(start = 16.dp, end = 16.dp, bottom = 92.dp), contentAlignment = Alignment.BottomEnd) {
        InventoryGradientButton(stringResource(R.string.inventory_add_material), onClick, icon = Icons.Outlined.Add)
    }
    }
}

@Composable
private fun InventoryGradientButton(
    text: String,
    onClick: () -> Unit,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .98f else 1f, BrandMotion.fast(), label = "inventoryButtonScale")
    Row(
        modifier = modifier
            .height(42.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(5.dp, RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFFFF7A00), Color(0xFFFF4C00))))
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        icon?.let { Icon(it, null, tint = Color.White, modifier = Modifier.size(18.dp)) }
        Text(text, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
    }
}

private fun InventoryFilter.labelRes(): Int = when (this) {
    InventoryFilter.ALL -> R.string.inventory_filter_all
    InventoryFilter.LOW_STOCK -> R.string.inventory_filter_low_stock
    InventoryFilter.ACTIVE -> R.string.inventory_filter_active
    InventoryFilter.INACTIVE -> R.string.inventory_filter_inactive
}

@Composable
private fun InventoryErrorContent(type: InventoryErrorType, onRetryClick: () -> Unit, modifier: Modifier) {
    ErrorState(
        title = stringResource(R.string.inventory_error_title),
        description = stringResource(when (type) {
            InventoryErrorType.NETWORK -> R.string.inventory_error_network
            InventoryErrorType.UNAUTHORIZED -> R.string.inventory_error_unauthorized
            InventoryErrorType.UNKNOWN -> R.string.inventory_error_unknown
        }),
        retryLabel = stringResource(R.string.retry),
        onRetry = onRetryClick,
        modifier = modifier,
    )
}

@Composable private fun inventoryDark(): Boolean = MaterialTheme.colorScheme.background.red < .2f
@Composable private fun inventoryPageColor(): Color = if (inventoryDark()) Color(0xFF070D14) else Color(0xFFFFFCFA)
@Composable private fun inventoryCardColor(): Color = if (inventoryDark()) Color(0xFF111A25) else Color.White
@Composable private fun inventorySecondary(): Color = if (inventoryDark()) Color(0xFFB2BBC6) else Color(0xFF6B6B6B)
@Composable private fun inventoryOutline(): Color = if (inventoryDark()) Color(0xFF283646) else Color(0xFFEEE8E3)
private fun Double.asNumberText(): String = NumberFormat.getNumberInstance().format(this)

@Preview(showBackground = true) @Composable private fun InventoryAdminPreview() { BrandCraftsTheme { InventoryScreen(InventoryUiState.Content("", previewInventoryItems()), true, {}) } }
@Preview(showBackground = true) @Composable private fun InventoryEmployeeDarkPreview() { BrandCraftsTheme(darkTheme = true) { InventoryScreen(InventoryUiState.Content("", previewInventoryItems()), false, {}) } }
private fun previewInventoryItems() = listOf(
    InventoryListItem("material-1", "Blue Vinyl", "VIN-BLU-01", "Vinyl", "rolls", 2.0, 5.0, 140.0, 220.0, true),
    InventoryListItem("material-2", "White Flex", "FLX-WHT-01", "Flex", "meters", 24.0, 6.0, 75.0, 120.0, true),
)
