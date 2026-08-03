package com.brandcrafts.erp.feature.purchaseorder

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.format.formatIndianCurrency
import com.brandcrafts.erp.ui.components.AppTopBar
import com.brandcrafts.erp.ui.components.EmptyState
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import java.math.BigDecimal
import java.text.DateFormat
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
    Column(modifier = modifier.fillMaxSize()) {
        if (operationInProgress || state.loading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        if (state.error) PurchaseOrderDetailsInlineError { onEvent(PurchaseOrderDetailsUiEvent.Retry) }
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { PurchaseOrderHeader(details) }
            item { PurchaseOrderSupplierCard(details.supplier) }
            item { PurchaseOrderDatesCard(details) }
            item { DetailsSectionTitle(stringResource(R.string.purchase_order_items_section)) }
            item { PurchaseOrderItemsTable(details.lines) }
            item { PurchaseOrderTotalCard(details.total) }
            if (details.remarks.isNotBlank()) item { PurchaseOrderRemarksCard(details.remarks) }
            if (state.approvedCancellationUnavailable) {
                item {
                    Text(
                        text = stringResource(R.string.purchase_order_error_approved_cancel_unsupported),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        PurchaseOrderDetailsActions(
            details = details,
            enabled = !operationInProgress && !state.loading,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun PurchaseOrderHeader(details: PurchaseOrderDetailsUi) {
    PurchaseOrderSurface(color = detailsHeaderColor()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = details.number.ifBlank { stringResource(R.string.purchase_order_value_unavailable) },
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = details.supplier?.company?.takeIf(String::isNotBlank)
                    ?: details.supplier?.name
                    ?: stringResource(R.string.purchase_order_supplier_unavailable),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            details.dateMillis?.let { date ->
                Text(
                    text = stringResource(R.string.purchase_order_created_on, formatDate(date)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun PurchaseOrderSupplierCard(supplier: PurchaseOrderSupplierOption?) {
    PurchaseOrderSurface {
        DetailsSectionTitle(stringResource(R.string.purchase_order_supplier_section))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Outlined.PersonOutline,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = Color(0xFFFF6500),
            )
            Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                Text(
                    supplier?.name ?: stringResource(R.string.purchase_order_supplier_unavailable),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                )
                supplier?.company?.takeIf(String::isNotBlank)?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(
                imageVector = Icons.Outlined.Phone,
                contentDescription = null,
                modifier = Modifier.size(17.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun PurchaseOrderDatesCard(details: PurchaseOrderDetailsUi) {
    PurchaseOrderSurface {
        DetailsSectionTitle(stringResource(R.string.purchase_order_information))
        details.dateMillis?.let { PurchaseOrderInformationRow(R.string.purchase_order_order_date, formatDate(it)) }
        details.expectedDeliveryDateMillis?.let { PurchaseOrderInformationRow(R.string.purchase_order_expected_delivery_label, formatDate(it)) }
    }
}

@Composable
private fun PurchaseOrderInformationRow(label: Int, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = Icons.Outlined.CalendarToday,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = Color(0xFFFF6500),
        )
        Text(
            text = stringResource(label),
            modifier = Modifier.padding(start = 8.dp).weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun PurchaseOrderItemsTable(lines: List<EditablePurchaseOrderLine>) {
    val dark = isPurchaseOrderDark()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = PurchaseOrderCardShape,
        color = if (dark) Color(0xFF111A25) else Color.White,
        border = BorderStroke(1.dp, if (dark) Color(0xFF283646) else Color(0xFFEEE8E3)),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (dark) Color(0xFF16212E) else Color(0xFFFFF8F4))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
            ) { PurchaseOrderTableHeader() }
            lines.forEachIndexed { index, line ->
                if (index > 0) HorizontalDivider(color = if (dark) Color(0xFF283646) else Color(0xFFF0E9E4))
                PurchaseOrderTableRow(line)
            }
        }
    }
}

@Composable
private fun PurchaseOrderTableHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        TableHeader(R.string.purchase_order_item, Modifier.weight(1f), TextAlign.Start)
        TableHeader(R.string.purchase_order_quantity_header, Modifier.width(58.dp), TextAlign.End)
        TableHeader(R.string.purchase_order_unit_price_header, Modifier.width(64.dp), TextAlign.End)
        TableHeader(R.string.purchase_order_amount_label, Modifier.width(68.dp), TextAlign.End)
    }
}

@Composable
private fun TableHeader(label: Int, modifier: Modifier, alignment: TextAlign) {
    Text(
        text = stringResource(label),
        modifier = modifier,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        textAlign = alignment,
    )
}

@Composable
private fun PurchaseOrderTableRow(line: EditablePurchaseOrderLine) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = line.description.ifBlank { stringResource(R.string.purchase_order_value_unavailable) },
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.SemiBold,
            )
            line.materialId?.takeIf(String::isNotBlank)?.let {
                Text(
                    text = stringResource(R.string.purchase_order_product_code, it),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            text = listOf(line.quantity.trim(), line.unit).filter(String::isNotBlank).joinToString(" "),
            modifier = Modifier.width(58.dp),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.End,
        )
        Text(
            text = line.unitPrice.toBigDecimalOrNull()?.let(::formatCurrency).orEmpty(),
            modifier = Modifier.width(64.dp),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.End,
        )
        Text(
            text = line.lineTotal?.let(::formatCurrency).orEmpty(),
            modifier = Modifier.width(68.dp),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun PurchaseOrderTotalCard(total: BigDecimal) {
    val dark = isPurchaseOrderDark()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = PurchaseOrderCardShape,
        color = if (dark) Color(0xFF3A1A0C) else Color(0xFFFFEEE2),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.purchase_order_total_amount),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = formatCurrency(total),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFF6500),
            )
        }
    }
}

@Composable
private fun PurchaseOrderRemarksCard(remarks: String) {
    PurchaseOrderSurface {
        DetailsSectionTitle(stringResource(R.string.purchase_order_remarks_section))
        Text(remarks, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun PurchaseOrderDetailsActions(
    details: PurchaseOrderDetailsUi,
    enabled: Boolean,
    onEvent: (PurchaseOrderDetailsUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (details.canApprove || details.canEdit || details.canCancel) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (details.canApprove) {
                    PurchaseOrderActionButton(
                        text = stringResource(R.string.purchase_order_approve),
                        icon = Icons.Outlined.Check,
                        onClick = { onEvent(PurchaseOrderDetailsUiEvent.ApproveClicked) },
                        modifier = Modifier.weight(1f),
                        enabled = enabled,
                        primary = true,
                    )
                }
                if (details.canEdit) {
                    PurchaseOrderActionButton(
                        text = stringResource(R.string.purchase_order_edit),
                        icon = Icons.Outlined.Edit,
                        onClick = { onEvent(PurchaseOrderDetailsUiEvent.EditClicked) },
                        modifier = Modifier.weight(1f),
                        enabled = enabled,
                    )
                }
                if (details.canCancel) {
                    PurchaseOrderActionButton(
                        text = stringResource(R.string.purchase_order_cancel),
                        icon = Icons.Outlined.Close,
                        onClick = { onEvent(PurchaseOrderDetailsUiEvent.CancelClicked) },
                        modifier = Modifier.weight(1f),
                        enabled = enabled,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PurchaseOrderActionButton(
                text = stringResource(R.string.purchase_order_preview_pdf),
                icon = Icons.Outlined.PictureAsPdf,
                onClick = { onEvent(PurchaseOrderDetailsUiEvent.PreviewPdfClicked) },
                modifier = Modifier.weight(1f),
                enabled = enabled,
            )
            PurchaseOrderActionButton(
                text = stringResource(R.string.purchase_order_share_pdf),
                icon = Icons.Outlined.Share,
                onClick = { onEvent(PurchaseOrderDetailsUiEvent.SharePdfClicked) },
                modifier = Modifier.weight(1f),
                enabled = enabled,
            )
        }
    }
}

@Composable
private fun PurchaseOrderActionButton(
    text: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    primary: Boolean = false,
) {
    val dark = isPurchaseOrderDark()
    Surface(
        modifier = modifier.clip(PurchaseOrderButtonShape).clickable(enabled = enabled, onClick = onClick),
        shape = PurchaseOrderButtonShape,
        color = if (primary) Color(0xFFFF6500) else if (dark) Color(0xFF111A25) else Color.White,
        border = if (primary) null else BorderStroke(1.dp, if (dark) Color(0xFF283646) else Color(0xFFEEE8E3)),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, modifier = Modifier.size(15.dp), tint = if (primary) Color.White else MaterialTheme.colorScheme.onSurface)
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
private fun PurchaseOrderDetailsInlineError(onRetry: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(R.string.purchase_order_error_description),
            modifier = Modifier.weight(1f),
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
        )
        TextButton(onClick = onRetry) { Text(stringResource(R.string.retry)) }
    }
}

@Composable
private fun PurchaseOrderSurface(
    color: Color? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val dark = isPurchaseOrderDark()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = PurchaseOrderCardShape,
        color = color ?: if (dark) Color(0xFF111A25) else Color.White,
        border = BorderStroke(1.dp, if (dark) Color(0xFF283646) else Color(0xFFEEE8E3)),
        shadowElevation = if (dark) 0.dp else 2.dp,
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
    }
}

@Composable
private fun DetailsSectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
}

@Composable
private fun isPurchaseOrderDark(): Boolean = MaterialTheme.colorScheme.background.red < .2f

@Composable
private fun detailsHeaderColor(): Color = if (isPurchaseOrderDark()) Color(0xFF111A25) else Color(0xFFFFF8F4)

private val PurchaseOrderCardShape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
private val PurchaseOrderButtonShape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)

private fun formatDate(value: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US).format(Date(value))

private fun formatCurrency(value: BigDecimal): String = formatIndianCurrency(value)
