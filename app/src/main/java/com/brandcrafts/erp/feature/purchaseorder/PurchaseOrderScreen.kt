package com.brandcrafts.erp.feature.purchaseorder

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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
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
import com.brandcrafts.erp.domain.model.PurchaseOrderStatus
import com.brandcrafts.erp.ui.components.EmptyState
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import com.brandcrafts.erp.ui.components.SearchBar
import com.brandcrafts.erp.ui.components.SectionHeader
import com.brandcrafts.erp.ui.components.StatusChip
import com.brandcrafts.erp.ui.components.StatusTone
import java.math.BigDecimal
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

@Composable
fun PurchaseOrderScreen(
    state: PurchaseOrderUiState,
    onEvent: (PurchaseOrderUiEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SectionHeader(
                title = stringResource(R.string.purchase_order_title),
                modifier = Modifier.padding(horizontal = 16.dp),
                actionLabel = stringResource(R.string.purchase_order_create),
                onActionClick = { onEvent(PurchaseOrderUiEvent.CreateClicked) },
            )
            SearchBar(
                query = state.query,
                onQueryChange = { onEvent(PurchaseOrderUiEvent.SearchChanged(it)) },
                placeholder = stringResource(R.string.purchase_order_search),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            PurchaseOrderStatusFilter(
                selected = state.status,
                onStatusSelected = { onEvent(PurchaseOrderUiEvent.StatusChanged(it)) },
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            if (state.refreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            when {
                state.loading && state.orders.isEmpty() -> LoadingView(
                    message = stringResource(R.string.purchase_order_loading),
                )
                state.error && state.orders.isEmpty() -> ErrorState(
                    title = stringResource(R.string.purchase_order_error),
                    description = stringResource(R.string.purchase_order_error_description),
                    retryLabel = stringResource(R.string.retry),
                    onRetry = { onEvent(PurchaseOrderUiEvent.Retry) },
                )
                state.orders.isEmpty() -> EmptyState(
                    title = stringResource(
                        if (state.query.isBlank() && state.status == null) {
                            R.string.purchase_order_empty
                        } else {
                            R.string.purchase_order_no_results
                        },
                    ),
                    description = stringResource(R.string.purchase_order_empty_description),
                )
                else -> {
                    if (state.error) {
                        PurchaseOrderInlineError(onRetry = { onEvent(PurchaseOrderUiEvent.Retry) })
                    }
                    PurchaseOrderList(
                        orders = state.orders,
                        approvingId = state.approvingId,
                        cancellingId = state.cancellingId,
                        onEvent = onEvent,
                    )
                }
            }
        }
    }
}

@Composable
private fun PurchaseOrderStatusFilter(
    selected: PurchaseOrderStatus?,
    onStatusSelected: (PurchaseOrderStatus?) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        FilterChip(
            selected = selected == null,
            onClick = { onStatusSelected(null) },
            label = { Text(stringResource(R.string.purchase_order_all_statuses)) },
        )
        PurchaseOrderStatus.entries.forEach { status ->
            FilterChip(
                selected = selected == status,
                onClick = { onStatusSelected(status) },
                label = { Text(stringResource(status.labelRes())) },
            )
        }
    }
}

@Composable
private fun PurchaseOrderInlineError(onRetry: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.purchase_order_error_description),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun PurchaseOrderList(
    orders: List<PurchaseOrderListItemUi>,
    approvingId: String?,
    cancellingId: String?,
    onEvent: (PurchaseOrderUiEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = orders, key = PurchaseOrderListItemUi::id) { purchaseOrder ->
            PurchaseOrderListItem(
                item = purchaseOrder,
                approving = approvingId == purchaseOrder.id,
                cancelling = cancellingId == purchaseOrder.id,
                onOpen = { onEvent(PurchaseOrderUiEvent.Clicked(purchaseOrder.id)) },
                onEdit = { onEvent(PurchaseOrderUiEvent.EditClicked(purchaseOrder.id)) },
                onApprove = { onEvent(PurchaseOrderUiEvent.ApproveClicked(purchaseOrder.id)) },
                onCancel = { onEvent(PurchaseOrderUiEvent.CancelClicked(purchaseOrder.id)) },
            )
        }
    }
}

@Composable
private fun PurchaseOrderListItem(
    item: PurchaseOrderListItemUi,
    approving: Boolean,
    cancelling: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onApprove: () -> Unit,
    onCancel: () -> Unit,
) {
    val operationInProgress = approving || cancelling
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        ListItem(
            headlineContent = { Text(item.number) },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(item.supplierName ?: stringResource(R.string.purchase_order_supplier_unavailable))
                    item.dateMillis?.let { Text(stringResource(R.string.purchase_order_date, formatDate(it))) }
                    item.expectedDeliveryDateMillis?.let {
                        Text(stringResource(R.string.purchase_order_expected_delivery, formatDate(it)))
                    }
                    Text(stringResource(R.string.purchase_order_total, formatCurrency(item.total)))
                }
            },
            trailingContent = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    StatusChip(label = stringResource(item.status.labelRes()), tone = item.status.tone())
                    if (approving || cancelling) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                        Text(
                            text = stringResource(
                                if (approving) R.string.purchase_order_approving else R.string.purchase_order_cancelling,
                            ),
                            style = MaterialTheme.typography.labelSmall,
                        )
                    } else {
                        if (item.canEdit) {
                            TextButton(onClick = onEdit, enabled = !operationInProgress) {
                                Text(stringResource(R.string.purchase_order_edit))
                            }
                        }
                        if (item.canApprove) {
                            TextButton(onClick = onApprove, enabled = !operationInProgress) {
                                Text(stringResource(R.string.purchase_order_approve))
                            }
                        }
                        if (item.canCancel) {
                            TextButton(onClick = onCancel, enabled = !operationInProgress) {
                                Text(stringResource(R.string.purchase_order_cancel))
                            }
                        }
                    }
                }
            },
        )
    }
}

private fun PurchaseOrderStatus.labelRes(): Int = when (this) {
    PurchaseOrderStatus.DRAFT -> R.string.purchase_order_status_draft
    PurchaseOrderStatus.APPROVED -> R.string.purchase_order_status_approved
    PurchaseOrderStatus.CANCELLED -> R.string.purchase_order_status_cancelled
}

private fun PurchaseOrderStatus.tone(): StatusTone = when (this) {
    PurchaseOrderStatus.DRAFT -> StatusTone.NEUTRAL
    PurchaseOrderStatus.APPROVED -> StatusTone.SUCCESS
    PurchaseOrderStatus.CANCELLED -> StatusTone.ERROR
}

private fun formatDate(value: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(value))

private fun formatCurrency(value: BigDecimal): String =
    NumberFormat.getCurrencyInstance().format(value)
