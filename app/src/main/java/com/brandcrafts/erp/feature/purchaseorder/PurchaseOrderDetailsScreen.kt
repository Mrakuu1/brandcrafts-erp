package com.brandcrafts.erp.feature.purchaseorder

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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.brandcrafts.erp.domain.model.PurchaseOrderStatus
import com.brandcrafts.erp.ui.components.AppTopBar
import com.brandcrafts.erp.ui.components.EmptyState
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import com.brandcrafts.erp.ui.components.OutlinedButton
import com.brandcrafts.erp.ui.components.PrimaryButton
import com.brandcrafts.erp.ui.components.SectionHeader
import com.brandcrafts.erp.ui.components.StatusChip
import com.brandcrafts.erp.ui.components.StatusTone
import java.math.BigDecimal
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

@Composable
fun PurchaseOrderDetailsScreen(
    state: PurchaseOrderDetailsUiState,
    onEvent: (PurchaseOrderDetailsUiEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.purchase_order_details_title),
                navigationIcon = Icons.Outlined.ArrowBack,
                navigationContentDescription = stringResource(R.string.purchase_order_back),
                onNavigationClick = { onEvent(PurchaseOrderDetailsUiEvent.BackClicked) },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        when {
            state.loading && state.details == null -> LoadingView(
                message = stringResource(R.string.purchase_order_loading),
                modifier = Modifier.padding(innerPadding),
            )
            state.error && state.details == null -> ErrorState(
                title = stringResource(R.string.purchase_order_error),
                description = stringResource(R.string.purchase_order_error_description),
                retryLabel = stringResource(R.string.retry),
                onRetry = { onEvent(PurchaseOrderDetailsUiEvent.Retry) },
                secondaryActionLabel = stringResource(R.string.cancel),
                onSecondaryAction = { onEvent(PurchaseOrderDetailsUiEvent.BackClicked) },
                modifier = Modifier.padding(innerPadding),
            )
            state.details != null -> PurchaseOrderDetailsContent(
                state = state,
                onEvent = onEvent,
                modifier = Modifier.padding(innerPadding),
            )
            else -> EmptyState(
                title = stringResource(R.string.purchase_order_error),
                description = stringResource(R.string.purchase_order_error_description),
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun PurchaseOrderDetailsContent(
    state: PurchaseOrderDetailsUiState,
    onEvent: (PurchaseOrderDetailsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val details = requireNotNull(state.details)
    val operationInProgress = state.approving || state.cancelling || state.pdfGenerating
    Column(modifier = modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (operationInProgress || state.loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (state.error) {
            PurchaseOrderDetailsInlineError(onRetry = { onEvent(PurchaseOrderDetailsUiEvent.Retry) })
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { PurchaseOrderHeader(details) }
            item { PurchaseOrderSupplierCard(details.supplier) }
            item { PurchaseOrderDatesCard(details) }
            item { SectionHeader(title = stringResource(R.string.purchase_order_items_section)) }
            items(
                items = details.lines,
                key = { line -> line.lineId ?: line.localId },
            ) { line -> PurchaseOrderDetailsLineItem(line) }
            item { PurchaseOrderGrandTotalCard(details.total) }
            if (details.remarks.isNotBlank()) {
                item { PurchaseOrderRemarksCard(details.remarks) }
            }
            if (state.approvedCancellationUnavailable) {
                item {
                    Text(
                        text = stringResource(R.string.purchase_order_error_approved_cancel_unsupported),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        PurchaseOrderDetailsActions(
            details = details,
            approving = state.approving,
            cancelling = state.cancelling,
            enabled = !operationInProgress && !state.loading,
            pdfGenerating = state.pdfGenerating,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun PurchaseOrderHeader(details: PurchaseOrderDetailsUi) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = details.number.ifBlank { stringResource(R.string.purchase_order_value_unavailable) },
                style = MaterialTheme.typography.headlineSmall,
            )
            StatusChip(label = stringResource(details.status.labelRes()), tone = details.status.tone())
            Text(
                text = stringResource(R.string.purchase_order_status_label, stringResource(details.status.labelRes())),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun PurchaseOrderSupplierCard(supplier: PurchaseOrderSupplierOption?) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(R.string.purchase_order_supplier_section), style = MaterialTheme.typography.titleMedium)
            Text(supplier?.name ?: stringResource(R.string.purchase_order_supplier_unavailable))
            supplier?.company?.takeIf(String::isNotBlank)?.let { company ->
                Text(company, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PurchaseOrderDatesCard(details: PurchaseOrderDetailsUi) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(R.string.purchase_order_dates_section), style = MaterialTheme.typography.titleMedium)
            details.dateMillis?.let { date ->
                Text(stringResource(R.string.purchase_order_date, formatDate(date)))
            }
            details.expectedDeliveryDateMillis?.let { date ->
                Text(stringResource(R.string.purchase_order_expected_delivery, formatDate(date)))
            }
        }
    }
}

@Composable
private fun PurchaseOrderDetailsLineItem(line: EditablePurchaseOrderLine) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = line.description.ifBlank { stringResource(R.string.purchase_order_value_unavailable) },
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                stringResource(
                    R.string.purchase_order_quantity_value,
                    line.quantity.trim().ifBlank { stringResource(R.string.purchase_order_value_unavailable) },
                    line.unit.ifBlank { stringResource(R.string.purchase_order_value_unavailable) },
                ),
            )
            Text(
                stringResource(
                    R.string.purchase_order_unit_price_value,
                    line.unitPrice.toBigDecimalOrNull()?.let(::formatCurrency)
                        ?: stringResource(R.string.purchase_order_value_unavailable),
                ),
            )
            line.lineTotal?.let { total ->
                Text(stringResource(R.string.purchase_order_line_total_label, formatCurrency(total)))
            }
        }
    }
}

@Composable
private fun PurchaseOrderGrandTotalCard(total: BigDecimal) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(stringResource(R.string.purchase_order_grand_total_label), style = MaterialTheme.typography.titleMedium)
            Text(formatCurrency(total), style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun PurchaseOrderRemarksCard(remarks: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(R.string.purchase_order_remarks_section), style = MaterialTheme.typography.titleMedium)
            Text(remarks)
        }
    }
}

@Composable
private fun PurchaseOrderDetailsActions(
    details: PurchaseOrderDetailsUi,
    approving: Boolean,
    cancelling: Boolean,
    enabled: Boolean,
    pdfGenerating: Boolean,
    onEvent: (PurchaseOrderDetailsUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (details.canApprove) {
            PrimaryButton(
                text = stringResource(R.string.purchase_order_approve),
                onClick = { onEvent(PurchaseOrderDetailsUiEvent.ApproveClicked) },
                enabled = enabled,
                loading = approving,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (details.canEdit) {
            OutlinedButton(
                text = stringResource(R.string.purchase_order_edit),
                onClick = { onEvent(PurchaseOrderDetailsUiEvent.EditClicked) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (details.canCancel) {
            OutlinedButton(
                text = stringResource(R.string.purchase_order_cancel),
                onClick = { onEvent(PurchaseOrderDetailsUiEvent.CancelClicked) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedButton(
            text = stringResource(R.string.purchase_order_preview_pdf),
            onClick = { onEvent(PurchaseOrderDetailsUiEvent.PreviewPdfClicked) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            text = stringResource(R.string.purchase_order_share_pdf),
            onClick = { onEvent(PurchaseOrderDetailsUiEvent.SharePdfClicked) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        if (pdfGenerating) {
            Text(stringResource(R.string.purchase_order_generating_pdf), style = MaterialTheme.typography.labelMedium)
        }
        if (cancelling) {
            Text(
                text = stringResource(R.string.purchase_order_cancelling),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

@Composable
private fun PurchaseOrderDetailsInlineError(onRetry: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.purchase_order_error_description),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
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
    DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US).format(Date(value))

private fun formatCurrency(value: BigDecimal): String =
    NumberFormat.getCurrencyInstance(Locale.US).format(value)
