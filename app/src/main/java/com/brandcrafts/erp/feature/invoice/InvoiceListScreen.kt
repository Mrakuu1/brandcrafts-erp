package com.brandcrafts.erp.feature.invoice

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.format.formatIndianCurrency
import com.brandcrafts.erp.domain.model.InvoicePaymentStatus
import com.brandcrafts.erp.domain.model.InvoiceStatus
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
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import java.math.BigDecimal
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun InvoiceListScreen(
    state: InvoiceListUiState,
    onEvent: (InvoiceListUiEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    var filtersOpen by remember { mutableStateOf(false) }
    var pendingDocumentStatus by remember(state.documentStatusFilter) {
        mutableStateOf(state.documentStatusFilter)
    }
    var pendingPaymentStatus by remember(state.paymentStatusFilter) {
        mutableStateOf(state.paymentStatusFilter)
    }
    OrdersListScaffold(
        query = state.searchQuery,
        onQueryChange = { onEvent(InvoiceListUiEvent.SearchChanged(it)) },
        searchPlaceholder = stringResource(R.string.invoice_search),
        refreshing = state.isRefreshing,
        onRefresh = { onEvent(InvoiceListUiEvent.Refresh) },
        isFilterActive = state.documentStatusFilter !is InvoiceDocumentStatusFilter.All ||
            state.paymentStatusFilter !is InvoicePaymentStatusFilter.All,
        onOpenFilters = {
            pendingDocumentStatus = state.documentStatusFilter
            pendingPaymentStatus = state.paymentStatusFilter
            filtersOpen = true
        },
        snackbarHostState = snackbarHostState,
        createAction = if (state.canCreate) {
            OrdersFabAction(stringResource(R.string.invoice_create)) {
                onEvent(InvoiceListUiEvent.CreateClicked)
            }
        } else {
            null
        },
        modifier = modifier,
    ) {
            when {
                state.content is InvoiceListContent.Loading && state.rows.isEmpty() -> LoadingView(
                    message = stringResource(R.string.invoice_loading),
                )
                state.content is InvoiceListContent.Error && state.rows.isEmpty() -> ErrorState(
                    title = stringResource(R.string.invoice_error),
                    description = stringResource(R.string.invoice_error_description),
                    retryLabel = stringResource(R.string.retry),
                    onRetry = { onEvent(InvoiceListUiEvent.Retry) },
                )
                state.rows.isEmpty() -> EmptyState(
                    title = stringResource(
                        if (state.searchQuery.isBlank() &&
                            state.documentStatusFilter is InvoiceDocumentStatusFilter.All &&
                            state.paymentStatusFilter is InvoicePaymentStatusFilter.All
                        ) {
                            R.string.invoice_empty
                        } else {
                            R.string.invoice_no_results
                        },
                    ),
                    description = stringResource(R.string.invoice_empty_description),
                    actionLabel = if (state.canCreate) stringResource(R.string.invoice_create) else null,
                    onActionClick = if (state.canCreate) {
                        { onEvent(InvoiceListUiEvent.CreateClicked) }
                    } else {
                        null
                    },
                )
                else -> {
                    if (state.content is InvoiceListContent.Error) {
                        InvoiceListInlineError(onRetry = { onEvent(InvoiceListUiEvent.Retry) })
                    }
                    InvoiceRows(
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
                onEvent(InvoiceListUiEvent.DocumentStatusFilterChanged(pendingDocumentStatus))
                onEvent(InvoiceListUiEvent.PaymentStatusFilterChanged(pendingPaymentStatus))
                filtersOpen = false
            },
        ) {
            InvoiceDocumentStatusFilters(
                selected = pendingDocumentStatus,
                onSelected = { pendingDocumentStatus = it },
            )
            InvoicePaymentStatusFilters(
                selected = pendingPaymentStatus,
                onSelected = { pendingPaymentStatus = it },
            )
        }
    }
}

@Composable
private fun InvoiceDocumentStatusFilters(
    selected: InvoiceDocumentStatusFilter,
    onSelected: (InvoiceDocumentStatusFilter) -> Unit,
) {
    OrdersFilterChoiceList(
        choices = buildList {
            add(
                OrdersFilterChoice(
                    id = "document-all",
                    label = stringResource(R.string.invoice_all_document_statuses),
                    selected = selected is InvoiceDocumentStatusFilter.All,
                    onSelected = { onSelected(InvoiceDocumentStatusFilter.All) },
                ),
            )
            InvoiceStatus.entries.forEach { status ->
                add(
                    OrdersFilterChoice(
                        id = "document-${status.name}",
                        label = stringResource(status.labelRes()),
                        selected = (selected as? InvoiceDocumentStatusFilter.Status)?.value == status,
                        onSelected = { onSelected(InvoiceDocumentStatusFilter.Status(status)) },
                    ),
                )
            }
        },
    )
}

@Composable
private fun InvoicePaymentStatusFilters(
    selected: InvoicePaymentStatusFilter,
    onSelected: (InvoicePaymentStatusFilter) -> Unit,
) {
    OrdersFilterChoiceList(
        choices = buildList {
            add(
                OrdersFilterChoice(
                    id = "payment-all",
                    label = stringResource(R.string.invoice_all_payment_statuses),
                    selected = selected is InvoicePaymentStatusFilter.All,
                    onSelected = { onSelected(InvoicePaymentStatusFilter.All) },
                ),
            )
            InvoicePaymentStatus.entries.forEach { status ->
                add(
                    OrdersFilterChoice(
                        id = "payment-${status.name}",
                        label = stringResource(status.labelRes()),
                        selected = (selected as? InvoicePaymentStatusFilter.Status)?.value == status,
                        onSelected = { onSelected(InvoicePaymentStatusFilter.Status(status)) },
                    ),
                )
            }
        },
    )
}

@Composable
private fun InvoiceListInlineError(onRetry: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.invoice_error_description),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun InvoiceRows(
    rows: List<InvoiceListItem>,
    actionInProgress: InvoiceListOperation?,
    onEvent: (InvoiceListUiEvent) -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        items(items = rows, key = InvoiceListItem::id) { item ->
            InvoiceListCard(
                item = item,
                operation = actionInProgress?.takeIf { it.invoiceId == item.id },
                onOpen = { onEvent(InvoiceListUiEvent.DetailsClicked(item.id)) },
                onEdit = { onEvent(InvoiceListUiEvent.EditClicked(item.id)) },
                onIssue = { onEvent(InvoiceListUiEvent.IssueClicked(item.id)) },
                onCancel = { onEvent(InvoiceListUiEvent.CancelClicked(item.id)) },
                onRecordPayment = { onEvent(InvoiceListUiEvent.RecordPaymentClicked(item.id)) },
            )
        }
    }
}

@Composable
private fun InvoiceListCard(
    item: InvoiceListItem,
    operation: InvoiceListOperation?,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onIssue: () -> Unit,
    onCancel: () -> Unit,
    onRecordPayment: () -> Unit,
) {
    val operating = operation != null
    OrdersDocumentCard(onClick = onOpen) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            leadingContent = { OrdersDocumentLeadingIcon() },
            headlineContent = { Text(item.invoiceNumber, style = MaterialTheme.typography.titleMedium) },
            supportingContent = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(item.customerName ?: stringResource(R.string.invoice_error_customer_not_found))
                    InvoiceLabelValue(R.string.invoice_date, formatInvoiceDate(item.invoiceDateMillis))
                    item.dueDateMillis?.let { InvoiceLabelValue(R.string.invoice_due_date, formatInvoiceDate(it)) }
                    InvoiceLabelValue(R.string.invoice_grand_total, formatInvoiceCurrency(item.grandTotal))
                    InvoiceLabelValue(R.string.invoice_paid_amount, formatInvoiceCurrency(item.paidAmount))
                    InvoiceLabelValue(R.string.invoice_outstanding_amount, formatInvoiceCurrency(item.outstandingAmount))
                }
            },
            trailingContent = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    StatusChip(stringResource(item.status.labelRes()), tone = item.status.tone())
                    StatusChip(stringResource(item.paymentStatus.labelRes()), tone = item.paymentStatus.tone())
                    if (item.isOverdue) {
                        StatusChip(stringResource(R.string.invoice_overdue), tone = StatusTone.ERROR)
                    }
                    if (operating) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    }
                }
            },
        )
        if (!operating) {
            OrdersCardActions(
                buildList {
                    if (item.canEdit) add(OrdersCardAction(stringResource(R.string.invoice_edit), Icons.Outlined.Edit, onEdit))
                    if (item.canIssue) add(OrdersCardAction(stringResource(R.string.invoice_issue), Icons.Outlined.Edit, onIssue))
                    if (item.canCancel) add(OrdersCardAction(stringResource(R.string.invoice_cancel), Icons.Outlined.Edit, onCancel))
                    if (item.canRecordPayment) add(OrdersCardAction(stringResource(R.string.invoice_record_payment), Icons.Outlined.Edit, onRecordPayment))
                },
            )
        }
    }
}

@Composable
private fun InvoiceLabelValue(labelRes: Int, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(labelRes), style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun InvoiceStatus.labelRes(): Int = when (this) {
    InvoiceStatus.DRAFT -> R.string.invoice_status_draft
    InvoiceStatus.ISSUED -> R.string.invoice_status_issued
    InvoiceStatus.CANCELLED -> R.string.invoice_status_cancelled
}

private fun InvoiceStatus.tone(): StatusTone = when (this) {
    InvoiceStatus.DRAFT -> StatusTone.NEUTRAL
    InvoiceStatus.ISSUED -> StatusTone.INFO
    InvoiceStatus.CANCELLED -> StatusTone.ERROR
}

private fun InvoicePaymentStatus.labelRes(): Int = when (this) {
    InvoicePaymentStatus.UNPAID -> R.string.invoice_payment_status_unpaid
    InvoicePaymentStatus.PARTIALLY_PAID -> R.string.invoice_payment_status_partially_paid
    InvoicePaymentStatus.PAID -> R.string.invoice_payment_status_paid
}

private fun InvoicePaymentStatus.tone(): StatusTone = when (this) {
    InvoicePaymentStatus.UNPAID -> StatusTone.WARNING
    InvoicePaymentStatus.PARTIALLY_PAID -> StatusTone.INFO
    InvoicePaymentStatus.PAID -> StatusTone.SUCCESS
}

private fun formatInvoiceDate(value: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US).format(Date(value))

private fun formatInvoiceCurrency(value: BigDecimal): String =
    formatIndianCurrency(value)

@Preview(showBackground = true)
@Composable
private fun InvoiceListLoadedPreview() {
    BrandCraftsTheme {
        InvoiceListScreen(
            state = InvoiceListUiState(
                content = InvoiceListContent.Loaded,
                canCreate = true,
                rows = listOf(previewInvoice()),
            ),
            onEvent = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InvoiceListLoadingPreview() {
    BrandCraftsTheme {
        InvoiceListScreen(InvoiceListUiState(), {}, SnackbarHostState())
    }
}

@Preview(showBackground = true)
@Composable
private fun InvoiceListEmptyPreview() {
    BrandCraftsTheme {
        InvoiceListScreen(
            InvoiceListUiState(content = InvoiceListContent.Empty, canCreate = true),
            {},
            SnackbarHostState(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InvoiceListErrorPreview() {
    BrandCraftsTheme {
        InvoiceListScreen(InvoiceListUiState(content = InvoiceListContent.Error()), {}, SnackbarHostState())
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InvoiceListDarkPreview() {
    BrandCraftsTheme(darkTheme = true) {
        InvoiceListScreen(
            InvoiceListUiState(content = InvoiceListContent.Loaded, canCreate = true, rows = listOf(previewInvoice())),
            {},
            SnackbarHostState(),
        )
    }
}

private fun previewInvoice(): InvoiceListItem = InvoiceListItem(
    id = "preview-invoice",
    invoiceNumber = "INV-000001",
    customerId = "preview-customer",
    customerName = "Northwind Studio",
    invoiceDateMillis = 1_784_851_200_000L,
    dueDateMillis = 1_787_443_200_000L,
    status = InvoiceStatus.ISSUED,
    paymentStatus = InvoicePaymentStatus.PARTIALLY_PAID,
    grandTotal = BigDecimal("1250.00"),
    paidAmount = BigDecimal("500.00"),
    outstandingAmount = BigDecimal("750.00"),
    isOverdue = true,
    canEdit = false,
    canIssue = false,
    canCancel = false,
    canRecordPayment = true,
)
