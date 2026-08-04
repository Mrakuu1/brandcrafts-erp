package com.brandcrafts.erp.feature.deliverychallan

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.domain.model.DeliveryChallanStatus
import com.brandcrafts.erp.ui.components.EmptyState
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import com.brandcrafts.erp.ui.components.StatusChip
import com.brandcrafts.erp.ui.components.StatusTone
import com.brandcrafts.erp.feature.purchaseorder.OrdersFabAction
import com.brandcrafts.erp.feature.purchaseorder.OrdersFilterBottomSheet
import com.brandcrafts.erp.feature.purchaseorder.OrdersFilterChoice
import com.brandcrafts.erp.feature.purchaseorder.OrdersFilterChoiceList
import com.brandcrafts.erp.feature.purchaseorder.OrdersListScaffold
import com.brandcrafts.erp.feature.purchaseorder.OrdersDocumentCard
import com.brandcrafts.erp.feature.purchaseorder.OrdersDocumentLeadingIcon
import com.brandcrafts.erp.feature.purchaseorder.OrdersCardAction
import com.brandcrafts.erp.feature.purchaseorder.OrdersCardActions
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeliveryChallanListScreen(
    state: DeliveryChallanListUiState,
    onEvent: (DeliveryChallanListUiEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    var filtersOpen by remember { mutableStateOf(false) }
    var pendingStatus by remember(state.statusFilter) { mutableStateOf(state.statusFilter) }
    OrdersListScaffold(
        query = state.searchQuery,
        onQueryChange = { onEvent(DeliveryChallanListUiEvent.SearchChanged(it)) },
        searchPlaceholder = stringResource(R.string.delivery_challan_search),
        refreshing = state.isRefreshing,
        onRefresh = { onEvent(DeliveryChallanListUiEvent.Refresh) },
        isFilterActive = state.statusFilter != null,
        onOpenFilters = {
            pendingStatus = state.statusFilter
            filtersOpen = true
        },
        snackbarHostState = snackbarHostState,
        createAction = if (state.canCreateIndependent) {
            OrdersFabAction(stringResource(R.string.delivery_challan_create)) {
                onEvent(DeliveryChallanListUiEvent.CreateIndependentClicked)
            }
        } else {
            null
        },
        alternateCreateAction = if (state.canCreateFromInvoice) {
            OrdersFabAction(stringResource(R.string.delivery_challan_create_from_invoice)) {
                onEvent(DeliveryChallanListUiEvent.CreateFromInvoiceClicked)
            }
        } else {
            null
        },
        modifier = modifier,
    ) {
            when {
                state.content is DeliveryChallanListContent.Loading && state.rows.isEmpty() -> {
                    LoadingView(message = stringResource(R.string.delivery_challan_loading))
                }
                state.content is DeliveryChallanListContent.Error && state.rows.isEmpty() -> {
                    ErrorState(
                        title = stringResource(R.string.delivery_challan_error),
                        description = stringResource(R.string.delivery_challan_error_description),
                        retryLabel = stringResource(R.string.retry),
                        onRetry = { onEvent(DeliveryChallanListUiEvent.Retry) },
                    )
                }
                state.rows.isEmpty() -> {
                    EmptyState(
                        title = stringResource(
                            if (state.searchQuery.isBlank() && state.statusFilter == null) {
                                R.string.delivery_challan_empty_title
                            } else {
                                R.string.delivery_challan_no_results
                            },
                        ),
                        description = stringResource(R.string.delivery_challan_empty_description),
                        actionLabel = if (state.canCreateIndependent) {
                            stringResource(R.string.delivery_challan_create)
                        } else {
                            null
                        },
                        onActionClick = if (state.canCreateIndependent) {
                            { onEvent(DeliveryChallanListUiEvent.CreateIndependentClicked) }
                        } else {
                            null
                        },
                    )
                }
                else -> {
                    if (state.content is DeliveryChallanListContent.Error) {
                        DeliveryChallanInlineError(
                            onRetry = { onEvent(DeliveryChallanListUiEvent.Retry) },
                        )
                    }
                    DeliveryChallanRows(
                        rows = state.rows,
                        actionInProgress = state.actionInProgress,
                        onEvent = onEvent,
                    )
                }
            }
    }
    if (filtersOpen) {
        OrdersFilterBottomSheet(
            onDismissRequest = { filtersOpen = false },
            onApply = {
                onEvent(DeliveryChallanListUiEvent.StatusChanged(pendingStatus))
                filtersOpen = false
            },
        ) {
            DeliveryChallanStatusFilters(
                selected = pendingStatus,
                onSelected = { pendingStatus = it },
            )
        }
    }
}

@Composable
private fun DeliveryChallanStatusFilters(
    selected: DeliveryChallanStatus?,
    onSelected: (DeliveryChallanStatus?) -> Unit,
) {
    OrdersFilterChoiceList(
        choices = buildList {
            add(
                OrdersFilterChoice(
                    id = "all",
                    label = stringResource(R.string.delivery_challan_all_statuses),
                    selected = selected == null,
                    onSelected = { onSelected(null) },
                ),
            )
            DeliveryChallanStatus.entries.forEach { status ->
                add(
                    OrdersFilterChoice(
                        id = status.name,
                        label = stringResource(status.labelRes()),
                        selected = selected == status,
                        onSelected = { onSelected(status) },
                    ),
                )
            }
        },
    )
}

@Composable
private fun DeliveryChallanInlineError(onRetry: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.delivery_challan_error_description),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun DeliveryChallanRows(
    rows: List<DeliveryChallanListItem>,
    actionInProgress: DeliveryChallanListOperation?,
    onEvent: (DeliveryChallanListUiEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = rows, key = DeliveryChallanListItem::id) { row ->
            DeliveryChallanCard(
                row = row,
                operation = actionInProgress?.takeIf { it.challanId == row.id },
                onOpen = { onEvent(DeliveryChallanListUiEvent.DetailsClicked(row.id)) },
                onEdit = { onEvent(DeliveryChallanListUiEvent.EditClicked(row.id)) },
                onDispatch = { onEvent(DeliveryChallanListUiEvent.DispatchClicked(row.id)) },
                onCancel = { onEvent(DeliveryChallanListUiEvent.CancelClicked(row.id)) },
            )
        }
    }
}

@Composable
private fun DeliveryChallanCard(
    row: DeliveryChallanListItem,
    operation: DeliveryChallanListOperation?,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onDispatch: () -> Unit,
    onCancel: () -> Unit,
) {
    OrdersDocumentCard(onClick = onOpen) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = { OrdersDocumentLeadingIcon() },
            headlineContent = { Text(row.number, style = MaterialTheme.typography.titleMedium) },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.customerName?.let { Text(it) }
                    Text(formatDeliveryChallanDate(row.dateMillis))
                    row.sourceInvoiceNumber?.let {
                        Text(stringResource(R.string.delivery_challan_source_invoice, it))
                    }
                }
            },
            trailingContent = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    StatusChip(
                        label = stringResource(row.status.labelRes()),
                        tone = row.status.tone(),
                    )
                    if (operation != null) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            },
        )
        if (operation == null) {
            OrdersCardActions(
                buildList {
                    if (row.canEdit) add(OrdersCardAction(stringResource(R.string.delivery_challan_edit), Icons.Outlined.Edit, onEdit))
                    if (row.canDispatch) add(OrdersCardAction(stringResource(R.string.delivery_challan_dispatch), Icons.Outlined.Edit, onDispatch))
                    if (row.canCancel) add(OrdersCardAction(stringResource(R.string.cancel), Icons.Outlined.Cancel, onCancel))
                },
            )
        }
    }
}

private fun DeliveryChallanStatus.labelRes(): Int = when (this) {
    DeliveryChallanStatus.DRAFT -> R.string.delivery_challan_status_draft
    DeliveryChallanStatus.DISPATCHED -> R.string.delivery_challan_status_dispatched
    DeliveryChallanStatus.CANCELLED -> R.string.delivery_challan_status_cancelled
}

private fun DeliveryChallanStatus.tone(): StatusTone = when (this) {
    DeliveryChallanStatus.DRAFT -> StatusTone.NEUTRAL
    DeliveryChallanStatus.DISPATCHED -> StatusTone.SUCCESS
    DeliveryChallanStatus.CANCELLED -> StatusTone.ERROR
}

private fun formatDeliveryChallanDate(value: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US).format(Date(value))
