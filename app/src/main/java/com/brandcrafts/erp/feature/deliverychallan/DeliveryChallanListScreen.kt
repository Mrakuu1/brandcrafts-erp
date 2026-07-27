package com.brandcrafts.erp.feature.deliverychallan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.domain.model.DeliveryChallanStatus
import com.brandcrafts.erp.ui.components.AppTopBar
import com.brandcrafts.erp.ui.components.EmptyState
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import com.brandcrafts.erp.ui.components.SearchBar
import com.brandcrafts.erp.ui.components.StatusChip
import com.brandcrafts.erp.ui.components.StatusTone
import com.brandcrafts.erp.ui.components.TopBarAction
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
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.delivery_challan_title),
                actions = buildList {
                    add(
                        TopBarAction(
                            icon = Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.retry),
                            onClick = { onEvent(DeliveryChallanListUiEvent.Refresh) },
                        ),
                    )
                    if (state.canCreateIndependent) {
                        add(
                            TopBarAction(
                                icon = Icons.Outlined.Add,
                                contentDescription = stringResource(R.string.delivery_challan_create),
                                onClick = { onEvent(DeliveryChallanListUiEvent.CreateIndependentClicked) },
                            ),
                        )
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SearchBar(
                query = state.searchQuery,
                onQueryChange = { onEvent(DeliveryChallanListUiEvent.SearchChanged(it)) },
                placeholder = stringResource(R.string.delivery_challan_search),
                searchIcon = Icons.Outlined.Search,
                searchIconContentDescription = stringResource(R.string.delivery_challan_search),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            DeliveryChallanStatusFilters(
                selected = state.statusFilter,
                onSelected = { onEvent(DeliveryChallanListUiEvent.StatusChanged(it)) },
            )
            if (state.canCreateFromInvoice) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    TextButton(
                        onClick = { onEvent(DeliveryChallanListUiEvent.CreateFromInvoiceClicked) },
                    ) {
                        Text(stringResource(R.string.delivery_challan_create_from_invoice))
                    }
                }
            }
            if (state.isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
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
    }
}

@Composable
private fun DeliveryChallanStatusFilters(
    selected: DeliveryChallanStatus?,
    onSelected: (DeliveryChallanStatus?) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selected == null,
                onClick = { onSelected(null) },
                label = { Text(stringResource(R.string.delivery_challan_all_statuses)) },
            )
        }
        items(DeliveryChallanStatus.entries) { status ->
            FilterChip(
                selected = selected == status,
                onClick = { onSelected(status) },
                label = { Text(stringResource(status.labelRes())) },
            )
        }
    }
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        ListItem(
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
                    } else {
                        if (row.canEdit) {
                            TextButton(onClick = onEdit) { Text(stringResource(R.string.delivery_challan_edit)) }
                        }
                        if (row.canDispatch) {
                            TextButton(onClick = onDispatch) {
                                Text(stringResource(R.string.delivery_challan_dispatch))
                            }
                        }
                        if (row.canCancel) {
                            TextButton(onClick = onCancel) {
                                Text(stringResource(R.string.delivery_challan_cancel))
                            }
                        }
                    }
                }
            },
        )
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
