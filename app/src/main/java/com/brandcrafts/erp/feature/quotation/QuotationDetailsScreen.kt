package com.brandcrafts.erp.feature.quotation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.format.formatIndianCurrency
import com.brandcrafts.erp.domain.model.Quotation
import com.brandcrafts.erp.domain.model.QuotationStatus
import com.brandcrafts.erp.domain.usecase.quotation.QuotationTotals
import com.brandcrafts.erp.ui.components.AppTopBar
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import com.brandcrafts.erp.ui.components.OutlinedButton
import com.brandcrafts.erp.ui.components.DocumentDetailsCard
import com.brandcrafts.erp.ui.components.DocumentDetailsSectionTitle
import com.brandcrafts.erp.ui.components.DocumentDetailsValueRow
import com.brandcrafts.erp.ui.components.StatusChip
import com.brandcrafts.erp.ui.components.StatusTone

@Composable
fun QuotationDetailsScreen(
    state: QuotationDetailsUiState,
    onEvent: (QuotationDetailsUiEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.quotation_details_title),
                navigationIcon = Icons.Outlined.ArrowBack,
                navigationContentDescription = stringResource(R.string.quotation_form_back),
                onNavigationClick = { onEvent(QuotationDetailsUiEvent.Back) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            state.loading && state.quotation == null -> LoadingView(
                modifier = Modifier.padding(innerPadding),
                message = stringResource(R.string.quotation_details_loading),
            )
            state.error && state.quotation == null -> ErrorState(
                title = stringResource(R.string.quotation_details_error),
                description = stringResource(R.string.quotation_details_error_description),
                retryLabel = stringResource(R.string.retry),
                onRetry = { onEvent(QuotationDetailsUiEvent.Retry) },
                secondaryActionLabel = stringResource(R.string.cancel),
                onSecondaryAction = { onEvent(QuotationDetailsUiEvent.Back) },
                modifier = Modifier.padding(innerPadding),
            )
            state.quotation != null -> QuotationDetailsBody(
                quotation = state.quotation,
                customerName = state.customerName,
                customerSubtitle = state.customerSubtitle,
                totals = state.totals,
                pdfGenerating = state.pdfGenerating,
                onEvent = onEvent,
                modifier = Modifier.padding(innerPadding),
            )
            else -> ErrorState(
                title = stringResource(R.string.quotation_details_error),
                description = stringResource(R.string.quotation_details_error_description),
                retryLabel = stringResource(R.string.retry),
                onRetry = { onEvent(QuotationDetailsUiEvent.Retry) },
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun QuotationDetailsBody(
    quotation: Quotation,
    customerName: String?,
    customerSubtitle: String?,
    totals: QuotationTotals?,
    pdfGenerating: Boolean,
    onEvent: (QuotationDetailsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (pdfGenerating) LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { QuotationHeaderCard(quotation, customerName, customerSubtitle) }
            item { QuotationInformationCard(quotation) }
            item { DocumentDetailsSectionTitle(stringResource(R.string.quotation_lines)) }
            item { QuotationItemsTable(quotation) }
            item { QuotationFinancialCard(quotation, totals) }
            if (quotation.remarks.isNotBlank()) {
                item {
                    DocumentDetailsCard {
                            DocumentDetailsSectionTitle(stringResource(R.string.quotation_notes))
                            Text(quotation.remarks)
                    }
                }
            }
        }
        QuotationDetailsActions(
            draft = quotation.status == QuotationStatus.DRAFT,
            pdfGenerating = pdfGenerating,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun QuotationFinancialCard(quotation: Quotation, totals: QuotationTotals?) {
    val dark = MaterialTheme.colorScheme.background.red < .2f
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (dark) Color(0xFF111A25) else Color.White,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (dark) Color(0xFF283646) else Color(0xFFEEE8E3),
        ),
        shadowElevation = if (dark) 0.dp else 2.dp,
    ) {
        Column {
            totals?.let {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    QuotationFinancialRow(R.string.quotation_subtotal, formatIndianCurrency(it.subtotal))
                    QuotationFinancialRow(R.string.quotation_tax_total, formatIndianCurrency(it.tax))
                }
            }
            Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (dark) Color(0xFF3A1A0C) else Color(0xFFFFEEE2))
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
                Text(
                    stringResource(R.string.quotation_total_amount),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    formatIndianCurrency(quotation.grandTotal),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF6500),
                )
            }
        }
    }
}

@Composable
private fun QuotationFinancialRow(label: Int, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp, vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(label), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelMedium)
        Text(value, modifier = Modifier.width(96.dp), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

@Composable
private fun QuotationHeaderCard(quotation: Quotation, customerName: String?, customerSubtitle: String?) {
    val dark = MaterialTheme.colorScheme.background.red < .2f
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (dark) Color(0xFF111A25) else Color(0xFFFFF8F4),
        shadowElevation = if (dark) 0.dp else 2.dp,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
            ) {
                Text(
                    quotation.number,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                StatusChip(stringResource(quotation.status.detailsLabelRes()), tone = quotation.status.detailsTone())
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    listOfNotNull(customerName, customerSubtitle)
                        .joinToString(" - ")
                        .takeIf(String::isNotBlank)
                        ?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    quotation.dateMillis?.let {
                        Text(
                            stringResource(R.string.quotation_created_on, formatQuotationDetailsDate(it)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        stringResource(R.string.quotation_total_amount),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        formatIndianCurrency(quotation.grandTotal),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuotationInformationCard(quotation: Quotation) {
    DocumentDetailsCard {
        DocumentDetailsSectionTitle(stringResource(R.string.quotation_information))
        quotation.dateMillis?.let { date ->
            QuotationInformationRow(stringResource(R.string.quotation_date_label), formatQuotationDetailsDate(date))
        }
        quotation.validUntilMillis?.let { date ->
            QuotationInformationRow(stringResource(R.string.quotation_valid_until_label), formatQuotationDetailsDate(date))
        }
    }
}

@Composable
private fun QuotationInformationRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.CalendarToday, null, modifier = Modifier.size(15.dp), tint = Color(0xFFFF6500))
        Text(label, modifier = Modifier.padding(start = 8.dp).weight(1f), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun QuotationItemsTable(quotation: Quotation) {
    val dark = MaterialTheme.colorScheme.background.red < .2f
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (dark) Color(0xFF111A25) else Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (dark) Color(0xFF283646) else Color(0xFFEEE8E3)),
    ) {
        Column {
            Box(
                modifier = Modifier.fillMaxWidth().background(if (dark) Color(0xFF16212E) else Color(0xFFFFF8F4)).padding(horizontal = 12.dp, vertical = 7.dp),
            ) { QuotationTableHeader() }
            quotation.lines.forEachIndexed { index, line ->
                if (index > 0) HorizontalDivider(color = if (dark) Color(0xFF283646) else Color(0xFFF0E9E4))
                Box(Modifier.padding(horizontal = 12.dp)) {
                    QuotationTableRow(
                        description = line.description,
                        materialId = line.materialId,
                        quantity = line.quantity.toPlainString(),
                        unitPrice = formatIndianCurrency(line.unitPrice),
                        amount = formatIndianCurrency(line.total),
                    )
                }
            }
        }
    }
}

@Composable
private fun QuotationTableHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.quotation_item), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        Text(stringResource(R.string.quotation_quantity), modifier = Modifier.width(42.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
        Text(stringResource(R.string.quotation_unit_price), modifier = Modifier.width(70.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
        Text(stringResource(R.string.quotation_amount), modifier = Modifier.width(72.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
    }
}

@Composable
private fun QuotationTableRow(description: String, materialId: String, quantity: String, unitPrice: String, amount: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text(description, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
            materialId.takeIf(String::isNotBlank)?.let { Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        Text(quantity, modifier = Modifier.width(42.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
        Text(unitPrice, modifier = Modifier.width(70.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
        Text(amount, modifier = Modifier.width(72.dp), style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.End)
    }
}

@Composable
private fun QuotationDetailsActions(
    draft: Boolean,
    pdfGenerating: Boolean,
    onEvent: (QuotationDetailsUiEvent) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        QuotationActionButton(stringResource(R.string.quotation_preview_pdf), Icons.Outlined.PictureAsPdf, { onEvent(QuotationDetailsUiEvent.PreviewPdf) }, Modifier.weight(1f), enabled = !pdfGenerating)
        QuotationActionButton(stringResource(R.string.quotation_share_pdf), Icons.Outlined.Share, { onEvent(QuotationDetailsUiEvent.SharePdf) }, Modifier.weight(1f), enabled = !pdfGenerating)
        if (draft) {
            QuotationPrimaryActionButton(
                text = stringResource(R.string.quotation_edit),
                icon = Icons.Outlined.Edit,
                onClick = { onEvent(QuotationDetailsUiEvent.Edit) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun QuotationActionButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier, enabled: Boolean) {
    val dark = MaterialTheme.colorScheme.background.red < .2f
    Surface(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = if (dark) Color(0xFF111A25) else Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, if (dark) Color(0xFF283646) else Color(0xFFEEE8E3)),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(15.dp))
            Text(text, modifier = Modifier.padding(start = 5.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun QuotationPrimaryActionButton(
    text: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    modifier: Modifier,
) {
    Surface(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFFF6500),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, null, modifier = Modifier.size(15.dp), tint = Color.White)
            Text(
                text,
                modifier = Modifier.padding(start = 5.dp),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
            )
        }
    }
}

private fun QuotationStatus.detailsLabelRes(): Int = when (this) {
    QuotationStatus.DRAFT -> R.string.quotation_status_draft
    QuotationStatus.APPROVED -> R.string.quotation_status_approved
    QuotationStatus.REJECTED -> R.string.quotation_status_rejected
    QuotationStatus.EXPIRED -> R.string.quotation_status_expired
}

private fun QuotationStatus.detailsTone(): StatusTone = when (this) {
    QuotationStatus.DRAFT -> StatusTone.WARNING
    QuotationStatus.APPROVED -> StatusTone.SUCCESS
    QuotationStatus.REJECTED, QuotationStatus.EXPIRED -> StatusTone.ERROR
}

private fun formatQuotationDetailsDate(value: Long): String =
    java.text.DateFormat.getDateInstance(java.text.DateFormat.MEDIUM).format(java.util.Date(value))
