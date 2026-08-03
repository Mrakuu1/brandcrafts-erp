package com.brandcrafts.erp.feature.purchaseorder

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.ui.bottomsheet.BrandBottomSheet
import com.brandcrafts.erp.ui.LocalBottomChromeVisible
import com.brandcrafts.erp.ui.theme.BrandSpacing

data class OrdersFabAction(
    val label: String,
    val onClick: () -> Unit,
)

data class OrdersFilterChoice(
    val id: String,
    val label: String,
    val selected: Boolean,
    val onSelected: () -> Unit,
)

data class OrdersCardAction(
    val label: String,
    val icon: ImageVector,
    val onClick: () -> Unit,
)

/** Shared document-card shell: matches the Inventory list surface without owning document content. */
@Composable
fun OrdersDocumentCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val shape = RoundedCornerShape(12.dp)
    // An explicit opaque surface prevents any parent/container tint from
    // making light-mode document cards appear grey or peach.
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(if (ordersDark()) 0.dp else 2.dp, shape, clip = false)
            .clickable(onClick = onClick),
        shape = shape,
        color = ordersCardColor(),
        tonalElevation = 0.dp,
        shadowElevation = 0.dp,
        border = androidx.compose.foundation.BorderStroke(1.dp, ordersOutline()),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) { content() }
    }
}

/** Compact warm document tile shared by all Orders cards. */
@Composable
fun OrdersDocumentLeadingIcon() {
    val dark = ordersDark()
    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(11.dp))
            .background(if (dark) Color(0xFF3A2415) else Color(0xFFFFF0E7)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.Description,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
fun OrdersCardActions(actions: List<OrdersCardAction>) {
    if (actions.isEmpty()) return
    androidx.compose.material3.HorizontalDivider(color = ordersOutline())
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        actions.forEach { action ->
            Row(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 46.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(OrdersCardColor())
                    .clickable(onClick = action.onClick)
                    .padding(horizontal = 6.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(action.label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.primary)
                Icon(action.icon, null, modifier = Modifier.padding(start = 5.dp).size(18.dp), tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersListScaffold(
    query: String,
    onQueryChange: (String) -> Unit,
    searchPlaceholder: String,
    refreshing: Boolean,
    onRefresh: () -> Unit,
    isFilterActive: Boolean,
    onOpenFilters: () -> Unit,
    snackbarHostState: SnackbarHostState,
    createAction: OrdersFabAction? = null,
    alternateCreateAction: OrdersFabAction? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var createMenuExpanded by remember { mutableStateOf(false) }
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            AnimatedVisibility(
                visible = LocalBottomChromeVisible.current,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            ) {
                Box(Modifier.padding(bottom = 76.dp)) {
                    createAction?.let { action ->
                        AnimatedContent(targetState = action.label, label = "ordersFabLabel") { label ->
                            OrdersGradientFab(
                                label = label,
                                onClick = { if (alternateCreateAction == null) action.onClick() else createMenuExpanded = true },
                            )
                        }
                        if (alternateCreateAction != null) {
                            DeliveryChallanCreateOptionsSheet(
                                visible = createMenuExpanded,
                                manualLabel = action.label,
                                invoiceLabel = alternateCreateAction.label,
                                onDismiss = { createMenuExpanded = false },
                                onManual = { createMenuExpanded = false; action.onClick() },
                                onFromInvoice = { createMenuExpanded = false; alternateCreateAction.onClick() },
                            )
                        }
                    }
                }
            }
        },
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = BrandSpacing.Xl,
                            top = BrandSpacing.Xs,
                            end = BrandSpacing.Md,
                            bottom = BrandSpacing.Sm,
                        ),
                    horizontalArrangement = Arrangement.spacedBy(BrandSpacing.Sm),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    OrdersSearchField(
                        query = query,
                        onQueryChange = onQueryChange,
                        placeholder = searchPlaceholder,
                        modifier = Modifier.weight(1f),
                    )
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .shadow(if (ordersDark()) 0.dp else 3.dp, CircleShape, clip = false)
                            .clip(CircleShape)
                            .background(ordersCardColor())
                            .border(1.dp, if (isFilterActive) MaterialTheme.colorScheme.primary else ordersOutline(), CircleShape)
                            .clickable(onClick = onOpenFilters),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.FilterList,
                            contentDescription = stringResource(R.string.orders_filter_action),
                            tint = if (isFilterActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                ordersSecondary()
                            },
                        )
                    }
                }
                content()
            }
        }
    }
}

@Composable
private fun OrdersGradientFab(label: String, onClick: () -> Unit) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) .98f else 1f, label = "ordersFabScale")
    Box(
        modifier = Modifier
            .heightIn(min = 42.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .shadow(5.dp, RoundedCornerShape(14.dp), clip = false)
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFFFF7A00), Color(0xFFFF4C00))))
            .clickable(interactionSource = source, indication = null, onClick = onClick)
            .padding(horizontal = 15.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = Color.White)
        }
    }
}

@Composable
private fun OrdersSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(22.dp)
    androidx.compose.material3.TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.heightIn(min = 44.dp).shadow(if (ordersDark()) 0.dp else 3.dp, shape, clip = false).clip(shape),
        shape = shape,
        singleLine = true,
        placeholder = { Text(placeholder, style = MaterialTheme.typography.bodyMedium, color = ordersSecondary()) },
        leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = placeholder, tint = ordersSecondary(), modifier = Modifier.size(19.dp)) },
        trailingIcon = if (query.isNotEmpty()) {
            { IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Outlined.Close, contentDescription = null) } }
        } else null,
        colors = androidx.compose.material3.TextFieldDefaults.colors(
            focusedContainerColor = ordersCardColor(),
            unfocusedContainerColor = ordersCardColor(),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
        ),
    )
}

@Composable
private fun DeliveryChallanCreateOptionsSheet(
    visible: Boolean,
    manualLabel: String,
    invoiceLabel: String,
    onDismiss: () -> Unit,
    onManual: () -> Unit,
    onFromInvoice: () -> Unit,
) {
    if (!visible) return
    BrandBottomSheet(title = stringResource(R.string.delivery_challan_create_title), onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            CreateOptionRow(label = manualLabel, supporting = stringResource(R.string.delivery_challan_create_manually), onClick = onManual)
            CreateOptionRow(label = invoiceLabel, supporting = stringResource(R.string.delivery_challan_create_from_invoice_description), onClick = onFromInvoice)
        }
    }
}

@Composable
private fun CreateOptionRow(label: String, supporting: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(ordersSearchSurface())
            .border(1.dp, ordersOutline(), MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Outlined.ChevronRight, contentDescription = null)
    }
}

@Composable
private fun ordersSearchSurface(): Color = ordersCardColor()

@Composable
private fun ordersOutline(): Color = if (MaterialTheme.colorScheme.background.red < .2f) Color(0xFF283646) else Color(0xFFEEE8E3)

@Composable
private fun ordersDark(): Boolean = MaterialTheme.colorScheme.background.red < .2f

@Composable
private fun ordersCardColor(): Color = if (ordersDark()) Color(0xFF111A25) else Color.White

@Composable
private fun ordersSecondary(): Color = if (ordersDark()) Color(0xFFB2BBC6) else Color(0xFF6B6B6B)

@Composable
fun OrdersFilterBottomSheet(
    onDismissRequest: () -> Unit,
    onApply: () -> Unit,
    content: @Composable () -> Unit,
) {
    BrandBottomSheet(
        title = stringResource(R.string.orders_apply_filters),
        onDismissRequest = onDismissRequest,
        containerColor = ordersCardColor(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = BrandSpacing.Md, bottom = BrandSpacing.Sm),
            verticalArrangement = Arrangement.spacedBy(BrandSpacing.Md),
        ) {
            content()
            Box(
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(12.dp))
                    .background(Brush.horizontalGradient(listOf(Color(0xFFFF7A00), Color(0xFFFF4C00))))
                    .clickable(onClick = onApply),
                contentAlignment = Alignment.Center,
            ) { Text(stringResource(R.string.orders_apply_filters), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold), color = Color.White) }
        }
    }
}

@Composable
fun OrdersFilterChoiceList(
    choices: List<OrdersFilterChoice>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BrandSpacing.Xs),
    ) {
        choices.forEach { choice ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (choice.selected) MaterialTheme.colorScheme.primary.copy(alpha = if (ordersDark()) .22f else .1f) else Color.Transparent)
                    .clickable(onClick = choice.onSelected)
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = choice.label,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = if (choice.selected) FontWeight.Bold else FontWeight.Normal),
                    color = if (choice.selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable private fun OrdersCardColor(): Color = if (OrdersDark()) Color(0xFF111A25) else Color.White
@Composable private fun OrdersDark(): Boolean = MaterialTheme.colorScheme.background.red < .2f
