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
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.format.formatIndianCurrency
import com.brandcrafts.erp.domain.model.InvoicePaymentStatus
import com.brandcrafts.erp.domain.model.InvoiceStatus
import com.brandcrafts.erp.ui.components.AppTextField
import com.brandcrafts.erp.ui.components.AppTopBar
import com.brandcrafts.erp.ui.components.EmptyState
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import com.brandcrafts.erp.ui.components.OutlinedButton
import com.brandcrafts.erp.ui.components.PrimaryButton
import com.brandcrafts.erp.ui.components.SectionHeader
import com.brandcrafts.erp.ui.components.StatusChip
import com.brandcrafts.erp.ui.components.StatusTone
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import java.math.BigDecimal
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun InvoiceDetailsScreen(
    state: InvoiceDetailsUiState,
    onEvent: (InvoiceDetailsUiEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.invoice_details_title),
                navigationIcon = Icons.Outlined.ArrowBack,
                navigationContentDescription = stringResource(R.string.invoice_back),
                onNavigationClick = { onEvent(InvoiceDetailsUiEvent.Back) },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->
        when {
            state.content is InvoiceDetailsContent.Loading && state.invoice == null -> LoadingView(
                message = stringResource(R.string.invoice_loading),
                modifier = Modifier.padding(innerPadding),
            )
            state.content is InvoiceDetailsContent.Error && state.invoice == null -> ErrorState(
                title = stringResource(R.string.invoice_error),
                description = stringResource(R.string.invoice_error_description),
                retryLabel = stringResource(R.string.retry),
                onRetry = { onEvent(InvoiceDetailsUiEvent.Retry) },
                secondaryActionLabel = stringResource(R.string.cancel),
                onSecondaryAction = { onEvent(InvoiceDetailsUiEvent.Back) },
                modifier = Modifier.padding(innerPadding),
            )
            state.invoice != null -> InvoiceDetailsBody(
                state = state,
                onEvent = onEvent,
                modifier = Modifier.padding(innerPadding),
            )
            else -> EmptyState(
                title = stringResource(R.string.invoice_error),
                description = stringResource(R.string.invoice_error_description),
                modifier = Modifier.padding(innerPadding),
            )
        }
    }

    state.paymentEntry?.let { entry ->
        InvoicePaymentDialog(
            entry = entry,
            onAmountChanged = { onEvent(InvoiceDetailsUiEvent.PaymentAmountChanged(it)) },
            onConfirm = { onEvent(InvoiceDetailsUiEvent.PaymentSubmitClicked) },
            onDismiss = { onEvent(InvoiceDetailsUiEvent.PaymentDismissed) },
        )
    }
}

@Composable
private fun InvoiceDetailsBody(
    state: InvoiceDetailsUiState,
    onEvent: (InvoiceDetailsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val invoice = requireNotNull(state.invoice)
    val operating = state.operationInProgress != null || state.isPdfGenerating
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (operating) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item { InvoiceHeaderCard(invoice) }
            item { InvoiceCustomerCard(invoice.customer) }
            item { InvoiceDateCard(invoice) }
            item { SectionHeader(title = stringResource(R.string.invoice_items)) }
            items(items = invoice.lines, key = InvoiceDetailsLine::id) { line ->
                InvoiceDetailsLineCard(line)
            }
            item { InvoiceTotalsCard(invoice) }
            if (invoice.remarks.isNotBlank()) {
                item { InvoiceRemarksCard(invoice.remarks) }
            }
            if (invoice.hasSafeAuditDates()) {
                item { InvoiceAuditCard(invoice) }
            }
        }
        InvoiceDetailsActions(
            state = state,
            enabled = !operating,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun InvoiceHeaderCard(invoice: InvoiceDetailsModel) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(invoice.invoiceNumber, style = MaterialTheme.typography.headlineSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusChip(stringResource(invoice.status.labelRes()), tone = invoice.status.tone())
                StatusChip(stringResource(invoice.paymentStatus.labelRes()), tone = invoice.paymentStatus.tone())
                if (invoice.isOverdue) {
                    StatusChip(stringResource(R.string.invoice_overdue), tone = StatusTone.ERROR)
                }
            }
        }
    }
}

@Composable
private fun InvoiceCustomerCard(customer: InvoiceCustomerOption) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(R.string.invoice_customer), style = MaterialTheme.typography.titleMedium)
            Text(customer.label, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

@Composable
private fun InvoiceDateCard(invoice: InvoiceDetailsModel) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            InvoiceLabelValue(R.string.invoice_date, formatInvoiceDetailsDate(invoice.invoiceDateMillis))
            invoice.dueDateMillis?.let { dueDate ->
                InvoiceLabelValue(R.string.invoice_due_date, formatInvoiceDetailsDate(dueDate))
            }
        }
    }
}

@Composable
private fun InvoiceDetailsLineCard(line: InvoiceDetailsLine) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(line.description, style = MaterialTheme.typography.titleMedium)
            Text(stringResource(R.string.invoice_quantity_value, line.quantity.toPlainString(), line.unit))
            InvoiceLabelValue(R.string.invoice_unit_price, formatInvoiceDetailsCurrency(line.unitPrice))
            InvoiceLabelValue(
                R.string.invoice_discount_percent,
                stringResource(R.string.invoice_percentage_value, line.discountPercent.toPlainString()),
            )
            InvoiceLabelValue(
                R.string.invoice_tax_percent,
                stringResource(R.string.invoice_percentage_value, line.taxPercent.toPlainString()),
            )
            Text(
                text = stringResource(R.string.invoice_line_total, formatInvoiceDetailsCurrency(line.lineTotal)),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun InvoiceTotalsCard(invoice: InvoiceDetailsModel) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(stringResource(R.string.invoice_totals), style = MaterialTheme.typography.titleMedium)
            InvoiceLabelValue(R.string.invoice_subtotal, formatInvoiceDetailsCurrency(invoice.subtotal))
            InvoiceLabelValue(R.string.invoice_discount_total, formatInvoiceDetailsCurrency(invoice.discountTotal))
            InvoiceLabelValue(R.string.invoice_tax_total, formatInvoiceDetailsCurrency(invoice.taxTotal))
            InvoiceLabelValue(R.string.invoice_grand_total, formatInvoiceDetailsCurrency(invoice.grandTotal))
            InvoiceLabelValue(R.string.invoice_paid_amount, formatInvoiceDetailsCurrency(invoice.paidAmount))
            InvoiceLabelValue(R.string.invoice_outstanding_amount, formatInvoiceDetailsCurrency(invoice.outstandingAmount))
        }
    }
}

@Composable
private fun InvoiceRemarksCard(remarks: String) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(R.string.invoice_remarks), style = MaterialTheme.typography.titleMedium)
            Text(remarks)
        }
    }
}

@Composable
private fun InvoiceAuditCard(invoice: InvoiceDetailsModel) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(stringResource(R.string.invoice_audit), style = MaterialTheme.typography.titleMedium)
            invoice.createdAtMillis?.let { Text(stringResource(R.string.invoice_created_at, formatInvoiceDetailsDate(it))) }
            invoice.updatedAtMillis?.let { Text(stringResource(R.string.invoice_updated_at, formatInvoiceDetailsDate(it))) }
            invoice.issuedAtMillis?.let { Text(stringResource(R.string.invoice_issued_at, formatInvoiceDetailsDate(it))) }
            invoice.cancelledAtMillis?.let { Text(stringResource(R.string.invoice_cancelled_at, formatInvoiceDetailsDate(it))) }
        }
    }
}

@Composable
private fun InvoiceDetailsActions(
    state: InvoiceDetailsUiState,
    enabled: Boolean,
    onEvent: (InvoiceDetailsUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (state.canIssue) {
            PrimaryButton(
                text = stringResource(R.string.invoice_issue),
                onClick = { onEvent(InvoiceDetailsUiEvent.IssueClicked) },
                enabled = enabled,
                loading = state.operationInProgress == InvoiceDetailsOperation.ISSUE,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.canEdit) {
            OutlinedButton(
                text = stringResource(R.string.invoice_edit),
                onClick = { onEvent(InvoiceDetailsUiEvent.EditClicked) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.canRecordPayment) {
            PrimaryButton(
                text = stringResource(R.string.invoice_record_payment),
                onClick = { onEvent(InvoiceDetailsUiEvent.RecordPaymentClicked) },
                enabled = enabled,
                loading = state.operationInProgress == InvoiceDetailsOperation.RECORD_PAYMENT,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (state.canCancel) {
            OutlinedButton(
                text = stringResource(R.string.invoice_cancel),
                onClick = { onEvent(InvoiceDetailsUiEvent.CancelClicked) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        OutlinedButton(
            text = stringResource(R.string.invoice_preview_pdf),
            onClick = { onEvent(InvoiceDetailsUiEvent.PreviewPdfClicked) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
            text = stringResource(R.string.invoice_share_pdf),
            onClick = { onEvent(InvoiceDetailsUiEvent.SharePdfClicked) },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        )
        if (state.isPdfGenerating) {
            Text(stringResource(R.string.invoice_generating_pdf), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun InvoicePaymentDialog(
    entry: InvoicePaymentEntryUiState,
    onAmountChanged: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!entry.isSaving) onDismiss() },
        title = { Text(stringResource(R.string.invoice_record_payment)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                InvoiceLabelValue(
                    R.string.invoice_outstanding_amount,
                    formatInvoiceDetailsCurrency(entry.outstandingAmount),
                )
                AppTextField(
                    value = entry.amountInput,
                    onValueChange = onAmountChanged,
                    label = stringResource(R.string.invoice_payment_amount),
                    enabled = !entry.isSaving,
                    errorMessage = entry.amountError?.message(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
                if (entry.isSaving) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Text(stringResource(R.string.invoice_record_payment), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !entry.isSaving) {
                Text(stringResource(R.string.invoice_record_payment))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !entry.isSaving) { Text(stringResource(R.string.cancel)) }
        },
    )
}

@Composable
private fun InvoiceLabelValue(labelRes: Int, value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(labelRes), style = MaterialTheme.typography.labelMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

private fun InvoiceDetailsModel.hasSafeAuditDates(): Boolean =
    createdAtMillis != null || updatedAtMillis != null || issuedAtMillis != null || cancelledAtMillis != null

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

@Composable
private fun InvoicePaymentEntryError.message(): String = stringResource(
    when (this) {
        InvoicePaymentEntryError.REQUIRED,
        InvoicePaymentEntryError.INVALID_AMOUNT -> R.string.invoice_error_invalid_paid_amount
        InvoicePaymentEntryError.EXCEEDS_OUTSTANDING -> R.string.invoice_error_payment_exceeds_outstanding
    },
)

private fun formatInvoiceDetailsDate(value: Long): String =
    DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.US).format(Date(value))

private fun formatInvoiceDetailsCurrency(value: BigDecimal): String =
    formatIndianCurrency(value)

@Preview(showBackground = true)
@Composable
private fun InvoiceDetailsPreview() {
    BrandCraftsTheme {
        InvoiceDetailsScreen(
            state = previewInvoiceDetailsState(),
            onEvent = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InvoiceDetailsDarkPreview() {
    BrandCraftsTheme(darkTheme = true) {
        InvoiceDetailsScreen(
            state = previewInvoiceDetailsState(),
            onEvent = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InvoiceDetailsLoadingPreview() {
    BrandCraftsTheme {
        InvoiceDetailsScreen(InvoiceDetailsUiState(), {}, SnackbarHostState())
    }
}

@Preview(showBackground = true)
@Composable
private fun InvoicePaymentDialogPreview() {
    BrandCraftsTheme {
        InvoicePaymentDialog(
            entry = InvoicePaymentEntryUiState(
                invoiceId = "preview-invoice",
                grandTotal = BigDecimal("1250.00"),
                paidAmount = BigDecimal("500.00"),
                outstandingAmount = BigDecimal("750.00"),
                paymentStatus = InvoicePaymentStatus.PARTIALLY_PAID,
            ),
            onAmountChanged = {},
            onConfirm = {},
            onDismiss = {},
        )
    }
}

private fun previewInvoiceDetailsState(): InvoiceDetailsUiState = InvoiceDetailsUiState(
    content = InvoiceDetailsContent.Loaded,
    invoice = InvoiceDetailsModel(
        id = "preview-invoice",
        invoiceNumber = "INV-000001",
        customer = InvoiceCustomerOption("preview-customer", "Northwind Studio"),
        invoiceDateMillis = 1_784_851_200_000L,
        dueDateMillis = 1_787_443_200_000L,
        status = InvoiceStatus.ISSUED,
        paymentStatus = InvoicePaymentStatus.PARTIALLY_PAID,
        lines = listOf(
            InvoiceDetailsLine(
                id = "preview-line",
                materialId = "preview-material",
                description = "Premium print material",
                quantity = BigDecimal("2"),
                unit = "piece",
                unitPrice = BigDecimal("625.00"),
                discountPercent = BigDecimal("10"),
                taxPercent = BigDecimal("18"),
                lineSubtotal = BigDecimal("1250.00"),
                lineDiscount = BigDecimal("125.00"),
                taxableAmount = BigDecimal("1125.00"),
                lineTax = BigDecimal("202.50"),
                lineTotal = BigDecimal("1327.50"),
                sortOrder = 0,
            ),
        ),
        subtotal = BigDecimal("1250.00"),
        discountTotal = BigDecimal("125.00"),
        taxTotal = BigDecimal("202.50"),
        grandTotal = BigDecimal("1327.50"),
        paidAmount = BigDecimal("500.00"),
        outstandingAmount = BigDecimal("827.50"),
        isOverdue = false,
        remarks = "Customer requested priority handling.",
        createdAtMillis = 1_784_851_200_000L,
        updatedAtMillis = 1_784_937_600_000L,
        issuedAtMillis = 1_784_937_600_000L,
        cancelledAtMillis = null,
    ),
    canEdit = false,
    canIssue = false,
    canCancel = true,
    canRecordPayment = true,
)
