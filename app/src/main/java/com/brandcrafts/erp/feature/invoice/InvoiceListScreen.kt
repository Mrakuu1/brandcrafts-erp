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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.domain.model.InvoicePaymentStatus
import com.brandcrafts.erp.domain.model.InvoiceStatus
import com.brandcrafts.erp.ui.components.AppTopBar
import com.brandcrafts.erp.ui.components.EmptyState
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import com.brandcrafts.erp.ui.components.SearchBar
import com.brandcrafts.erp.ui.components.StatusChip
import com.brandcrafts.erp.ui.components.StatusTone
import com.brandcrafts.erp.ui.components.TopBarAction
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import java.math.BigDecimal
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

@Composable
fun InvoiceListScreen(
    state: InvoiceListUiState,
    onEvent: (InvoiceListUiEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.invoice_title),
                actions = buildList {
                    add(
                        TopBarAction(
                            icon = Icons.Outlined.Refresh,
                            contentDescription = stringResource(R.string.retry),
                            onClick = { onEvent(InvoiceListUiEvent.Refresh) },
                        ),
                    )
                    if (state.canCreate) {
                        add(
                            TopBarAction(
                                icon = Icons.Outlined.Add,
                                contentDescription = stringResource(R.string.invoice_create),
                                onClick = { onEvent(InvoiceListUiEvent.CreateClicked) },
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
                onQueryChange = { onEvent(InvoiceListUiEvent.SearchChanged(it)) },
                placeholder = stringResource(R.string.invoice_search),
                searchIcon = Icons.Outlined.Search,
                searchIconContentDescription = stringResource(R.string.invoice_search),
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            InvoiceDocumentStatusFilters(
                selected = state.documentStatusFilter,
                onSelected = { onEvent(InvoiceListUiEvent.DocumentStatusFilterChanged(it)) },
            )
            InvoicePaymentStatusFilters(
                selected = state.paymentStatusFilter,
                onSelected = { onEvent(InvoiceListUiEvent.PaymentStatusFilterChanged(it)) },
            )
            if (state.isRefreshing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
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
    }
}

@Composable
private fun InvoiceDocumentStatusFilters(
    selected: InvoiceDocumentStatusFilter,
    onSelected: (InvoiceDocumentStatusFilter) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selected is InvoiceDocumentStatusFilter.All,
                onClick = { onSelected(InvoiceDocumentStatusFilter.All) },
                label = { Text(stringResource(R.string.invoice_all_document_statuses)) },
            )
        }
        items(InvoiceStatus.entries) { status ->
            FilterChip(
                selected = (selected as? InvoiceDocumentStatusFilter.Status)?.value == status,
                onClick = { onSelected(InvoiceDocumentStatusFilter.Status(status)) },
                label = { Text(stringResource(status.labelRes())) },
            )
        }
    }
}

@Composable
private fun InvoicePaymentStatusFilters(
    selected: InvoicePaymentStatusFilter,
    onSelected: (InvoicePaymentStatusFilter) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(
                selected = selected is InvoicePaymentStatusFilter.All,
                onClick = { onSelected(InvoicePaymentStatusFilter.All) },
                label = { Text(stringResource(R.string.invoice_all_payment_statuses)) },
            )
        }
        items(InvoicePaymentStatus.entries) { status ->
            FilterChip(
                selected = (selected as? InvoicePaymentStatusFilter.Status)?.value == status,
                onClick = { onSelected(InvoicePaymentStatusFilter.Status(status)) },
                label = { Text(stringResource(status.labelRes())) },
            )
        }
    }
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
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onOpen,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        ListItem(
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
                    } else {
                        if (item.canEdit) {
                            TextButton(onClick = onEdit) { Text(stringResource(R.string.invoice_edit)) }
                        }
                        if (item.canIssue) {
                            TextButton(onClick = onIssue) { Text(stringResource(R.string.invoice_issue)) }
                        }
                        if (item.canCancel) {
                            TextButton(onClick = onCancel) { Text(stringResource(R.string.invoice_cancel)) }
                        }
                        if (item.canRecordPayment) {
                            TextButton(onClick = onRecordPayment) { Text(stringResource(R.string.invoice_record_payment)) }
                        }
                    }
                }
            },
        )
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
    NumberFormat.getCurrencyInstance(Locale.US).format(value)

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
