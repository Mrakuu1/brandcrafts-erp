package com.brandcrafts.erp.feature.invoice

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.font.FontWeight
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.format.formatIndianCurrency
import com.brandcrafts.erp.domain.model.InvoicePaymentStatus
import com.brandcrafts.erp.domain.model.InvoiceStatus
import com.brandcrafts.erp.ui.components.AppTextField
import com.brandcrafts.erp.ui.components.AppTopBar
import com.brandcrafts.erp.ui.components.EmptyState
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import com.brandcrafts.erp.ui.components.StatusChip
import com.brandcrafts.erp.ui.components.StatusTone
import com.brandcrafts.erp.ui.components.DocumentDetailsCard
import com.brandcrafts.erp.ui.components.DocumentDetailsSectionTitle
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import java.math.BigDecimal
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.text.KeyboardOptions
import com.brandcrafts.erp.ui.components.DocumentDetailsValueRow

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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { InvoiceHeaderCard(invoice) }
            item { InvoiceCustomerCard(invoice.customer) }
            item { InvoiceDateCard(invoice) }
            item { DocumentDetailsSectionTitle(stringResource(R.string.invoice_items)) }
            item { InvoiceItemsTable(invoice.lines) }
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
    val dark = MaterialTheme.colorScheme.background.red < .2f
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (dark) Color(0xFF111A25) else Color(0xFFFFF8F4),
        shadowElevation = if (dark) 0.dp else 2.dp,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(invoice.invoiceNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    StatusChip(stringResource(invoice.status.labelRes()), tone = invoice.status.tone())
                    StatusChip(stringResource(invoice.paymentStatus.labelRes()), tone = invoice.paymentStatus.tone())
                    if (invoice.isOverdue) StatusChip(stringResource(R.string.invoice_overdue), tone = StatusTone.ERROR)
                }
                Text(invoice.customer.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.invoice_created_at, formatInvoiceDetailsDate(invoice.createdAtMillis ?: invoice.invoiceDateMillis)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.invoice_grand_total), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatInvoiceDetailsCurrency(invoice.grandTotal), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.invoice_outstanding_amount), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(formatInvoiceDetailsCurrency(invoice.outstandingAmount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun InvoiceCustomerCard(customer: InvoiceCustomerOption) {
    DocumentDetailsCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DocumentDetailsSectionTitle(stringResource(R.string.invoice_customer))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Outlined.PersonOutline, null, tint = Color(0xFFFF6500), modifier = Modifier.size(18.dp))
                Text(customer.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun InvoiceDateCard(invoice: InvoiceDetailsModel) {
    DocumentDetailsCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DocumentDetailsSectionTitle(stringResource(R.string.invoice_information))
            InvoiceInformationRow(R.string.invoice_date, formatInvoiceDetailsDate(invoice.invoiceDateMillis))
            invoice.dueDateMillis?.let { dueDate ->
                InvoiceInformationRow(R.string.invoice_due_date, formatInvoiceDetailsDate(dueDate))
            }
        }
    }
}

@Composable
private fun InvoiceTableHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.invoice_item), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.invoice_quantity), modifier = Modifier.width(42.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
        Text(stringResource(R.string.invoice_unit_price), modifier = Modifier.width(70.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
        Text(stringResource(R.string.invoice_amount), modifier = Modifier.width(72.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

@Composable
private fun InvoiceItemsTable(lines: List<InvoiceDetailsLine>) {
    var expanded by remember(lines) { mutableStateOf(false) }
    val displayedLines = if (expanded) lines else lines.take(2)
    val dark = MaterialTheme.colorScheme.background.red < .2f
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (dark) Color(0xFF111A25) else Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (dark) Color(0xFF283646) else Color(0xFFEEE8E3)),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (dark) Color(0xFF16212E) else Color(0xFFFFF8F4))
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            ) { InvoiceTableHeader() }
            displayedLines.forEachIndexed { index, line ->
                if (index > 0) HorizontalDivider(color = if (dark) Color(0xFF283646) else Color(0xFFF0E9E4))
                Box(Modifier.padding(horizontal = 12.dp)) { InvoiceTableRow(line) }
            }
            if (lines.size > 2 && !expanded) {
                Text(
                    text = stringResource(R.string.invoice_view_all_items, lines.size),
                    modifier = Modifier
                        .padding(start = 12.dp, top = 2.dp, bottom = 9.dp)
                        .clickable { expanded = true },
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFFFF6500),
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun InvoiceTableRow(line: InvoiceDetailsLine) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(line.description, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            Text(line.unit, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text(line.quantity.toPlainString(), modifier = Modifier.width(42.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
        Text(formatInvoiceDetailsCurrency(line.unitPrice), modifier = Modifier.width(70.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
        Text(formatInvoiceDetailsCurrency(line.lineTotal), modifier = Modifier.width(72.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
    }
}

@Composable
private fun InvoiceInformationRow(label: Int, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.CalendarToday, null, modifier = Modifier.size(16.dp), tint = Color(0xFFFF6500))
        Text(
            text = stringResource(label),
            modifier = Modifier.padding(start = 8.dp).weight(1f),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun InvoiceDetailsLineCard(line: InvoiceDetailsLine) {
    DocumentDetailsCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
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
    val dark = MaterialTheme.colorScheme.background.red < .2f
    DocumentDetailsCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            InvoiceTotalValueRow(R.string.invoice_subtotal, formatInvoiceDetailsCurrency(invoice.subtotal))
            InvoiceTotalValueRow(R.string.invoice_discount_total, formatInvoiceDetailsCurrency(invoice.discountTotal))
            InvoiceTotalValueRow(R.string.invoice_tax_total, formatInvoiceDetailsCurrency(invoice.taxTotal))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (dark) Color(0xFF3A1A0C) else Color(0xFFFFEEE2))
                    .padding(horizontal = 8.dp, vertical = 5.dp),
            ) {
                InvoiceEmphasizedValueRow(R.string.invoice_grand_total, formatInvoiceDetailsCurrency(invoice.grandTotal), Color(0xFFFF6500))
            }
            InvoiceTotalValueRow(
                R.string.invoice_paid_amount,
                formatInvoiceDetailsCurrency(invoice.paidAmount),
                valueColor = Color(0xFF159447),
            )
            InvoiceTotalValueRow(
                R.string.invoice_outstanding_amount,
                formatInvoiceDetailsCurrency(invoice.outstandingAmount),
            )
        }
    }
}

@Composable
private fun InvoiceEmphasizedValueRow(label: Int, value: String, color: Color) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun InvoiceTotalValueRow(label: Int, value: String, valueColor: Color = MaterialTheme.colorScheme.onSurface) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(label),
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = value,
            modifier = Modifier.width(96.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun InvoiceRemarksCard(remarks: String) {
    DocumentDetailsCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DocumentDetailsSectionTitle(stringResource(R.string.invoice_remarks))
            Text(remarks)
        }
    }
}

@Composable
private fun InvoiceAuditCard(invoice: InvoiceDetailsModel) {
    DocumentDetailsCard {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            DocumentDetailsSectionTitle(stringResource(R.string.invoice_audit))
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
        if (state.canIssue || state.canEdit || state.canCancel) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.canIssue) {
                    InvoiceDetailsActionButton(
                        text = stringResource(R.string.invoice_issue),
                        onClick = { onEvent(InvoiceDetailsUiEvent.IssueClicked) },
                        icon = Icons.Outlined.Edit,
                        primary = true,
                        enabled = enabled,
                        loading = state.operationInProgress == InvoiceDetailsOperation.ISSUE,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (state.canEdit) {
                    InvoiceDetailsActionButton(
                        text = stringResource(R.string.invoice_edit),
                        onClick = { onEvent(InvoiceDetailsUiEvent.EditClicked) },
                        icon = Icons.Outlined.Edit,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    )
                }
                if (state.canCancel) {
                    InvoiceDetailsActionButton(
                        text = stringResource(R.string.invoice_cancel),
                        onClick = { onEvent(InvoiceDetailsUiEvent.CancelClicked) },
                        icon = Icons.Outlined.Edit,
                        enabled = enabled,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        if (state.canRecordPayment) {
            InvoiceDetailsActionButton(
                text = stringResource(R.string.invoice_record_payment),
                onClick = { onEvent(InvoiceDetailsUiEvent.RecordPaymentClicked) },
                icon = Icons.Outlined.Edit,
                primary = true,
                enabled = enabled,
                loading = state.operationInProgress == InvoiceDetailsOperation.RECORD_PAYMENT,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        InvoiceDetailsActionButton(
            text = stringResource(R.string.invoice_preview_pdf),
            onClick = { onEvent(InvoiceDetailsUiEvent.PreviewPdfClicked) },
            icon = Icons.Outlined.PictureAsPdf,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
        InvoiceDetailsActionButton(
            text = stringResource(R.string.invoice_share_pdf),
            onClick = { onEvent(InvoiceDetailsUiEvent.SharePdfClicked) },
            icon = Icons.Outlined.Share,
            enabled = enabled,
            modifier = Modifier.weight(1f),
        )
        }
        if (state.isPdfGenerating) {
            Text(stringResource(R.string.invoice_generating_pdf), style = MaterialTheme.typography.labelMedium)
        }
    }
}

@Composable
private fun InvoiceDetailsActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    primary: Boolean = false,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val dark = MaterialTheme.colorScheme.background.red < .2f
    val shape = RoundedCornerShape(8.dp)
    Surface(
        modifier = modifier
            .clip(shape)
            .then(
                if (primary) Modifier.background(Brush.horizontalGradient(listOf(Color(0xFFFF7A00), Color(0xFFFF4C00))), shape)
                else Modifier,
            )
            .clickable(enabled = enabled && !loading, onClick = onClick),
        shape = shape,
        color = if (primary) Color.Transparent else if (dark) Color(0xFF111A25) else Color.White,
        border = if (primary) null else androidx.compose.foundation.BorderStroke(1.dp, if (dark) Color(0xFF283646) else Color(0xFFEEE8E3)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) CircularProgressIndicator(modifier = Modifier.size(14.dp), color = Color.White, strokeWidth = 2.dp)
            else Icon(icon, null, modifier = Modifier.size(15.dp), tint = if (primary) Color.White else MaterialTheme.colorScheme.onSurface)
            Text(
                text = text,
                modifier = Modifier.padding(start = 5.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = if (primary) Color.White else MaterialTheme.colorScheme.onSurface,
            )
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
    DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US).format(Date(value))

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
