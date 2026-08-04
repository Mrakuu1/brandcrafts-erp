package com.brandcrafts.erp.feature.deliverychallan

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
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import com.brandcrafts.erp.domain.model.DeliveryChallanStatus
import com.brandcrafts.erp.ui.components.AppTopBar
import com.brandcrafts.erp.ui.components.EmptyState
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import com.brandcrafts.erp.ui.components.CenteredLoadingOverlay
import com.brandcrafts.erp.ui.components.StatusChip
import com.brandcrafts.erp.ui.components.StatusTone
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DeliveryChallanDetailsScreen(
    state: DeliveryChallanDetailsUiState,
    onEvent: (DeliveryChallanDetailsUiEvent) -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.delivery_challan_details_title),
                navigationIcon = Icons.Outlined.ArrowBack,
                navigationContentDescription = stringResource(R.string.delivery_challan_back),
                onNavigationClick = { onEvent(DeliveryChallanDetailsUiEvent.Back) },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when (state.content) {
            DeliveryChallanDetailsContent.Loading -> LoadingView(
                message = stringResource(R.string.delivery_challan_form_loading),
                modifier = Modifier.padding(innerPadding),
            )
            DeliveryChallanDetailsContent.Error -> ErrorState(
                title = stringResource(R.string.delivery_challan_error),
                description = stringResource(R.string.delivery_challan_error_description),
                retryLabel = stringResource(R.string.retry),
                onRetry = { onEvent(DeliveryChallanDetailsUiEvent.Retry) },
                secondaryActionLabel = stringResource(R.string.delivery_challan_back),
                onSecondaryAction = { onEvent(DeliveryChallanDetailsUiEvent.Back) },
                modifier = Modifier.padding(innerPadding),
            )
            DeliveryChallanDetailsContent.Loaded -> state.challan?.let { challan ->
                DeliveryChallanDetailsContent(
                    challan = challan,
                    canEdit = state.canEdit,
                    canDispatch = state.canDispatch,
                    canCancel = state.canCancel,
                    isOperating = state.isOperating,
                    isGeneratingPdf = state.isGeneratingPdf,
                    onEvent = onEvent,
                    modifier = Modifier.padding(innerPadding),
                )
            } ?: EmptyState(
                title = stringResource(R.string.delivery_challan_error),
                description = stringResource(R.string.delivery_challan_error_description),
                modifier = Modifier.padding(innerPadding),
            )
        }
    }
}

@Composable
private fun DeliveryChallanDetailsContent(
    challan: DeliveryChallanDetailsModel,
    canEdit: Boolean,
    canDispatch: Boolean,
    canCancel: Boolean,
    isOperating: Boolean,
    isGeneratingPdf: Boolean,
    onEvent: (DeliveryChallanDetailsUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    androidx.compose.foundation.layout.Box(modifier = modifier.fillMaxSize()) {
    Column(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 12.dp, top = 8.dp, end = 12.dp, bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item { DeliveryChallanHeader(challan) }
            item { DeliveryChallanCustomerCard(challan) }
            item { DeliveryChallanInformationCard(challan) }
            item { DetailsSectionTitle(stringResource(R.string.delivery_challan_items)) }
            item { DeliveryChallanItemsTable(challan.lines) }
            if (challan.notes.isNotBlank()) item { DeliveryChallanNotesCard(challan.notes) }
        }
        DeliveryChallanDetailsActions(
            canEdit = canEdit,
            canDispatch = canDispatch,
            canCancel = canCancel,
            enabled = !isOperating && !isGeneratingPdf,
            onEvent = onEvent,
        )
    }
        CenteredLoadingOverlay(visible = isOperating || isGeneratingPdf)
    }
}

@Composable
private fun DeliveryChallanHeader(challan: DeliveryChallanDetailsModel) {
    DeliveryChallanSurface(color = if (isDeliveryChallanDark()) Color(0xFF111A25) else Color(0xFFFFF8F4)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(challan.number, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(challan.customer.label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    stringResource(R.string.delivery_challan_created_on, formatDeliveryChallanDetailsDate(challan.dateMillis)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            StatusChip(stringResource(challan.status.labelRes()), tone = challan.status.tone())
        }
    }
}

@Composable
private fun DeliveryChallanCustomerCard(challan: DeliveryChallanDetailsModel) {
    DeliveryChallanSurface {
        DetailsSectionTitle(stringResource(R.string.delivery_challan_customer))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.PersonOutline, null, modifier = Modifier.size(18.dp), tint = Color(0xFFFF6500))
            Column(modifier = Modifier.padding(start = 10.dp).weight(1f)) {
                Text(challan.customer.label, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                Text(challan.deliveryAddress, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Outlined.Phone, null, modifier = Modifier.size(17.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun DeliveryChallanInformationCard(challan: DeliveryChallanDetailsModel) {
    DeliveryChallanSurface {
        DetailsSectionTitle(stringResource(R.string.delivery_challan_delivery_information))
        DeliveryChallanInfoRow(R.string.delivery_challan_delivery_date_label, formatDeliveryChallanDetailsDate(challan.dateMillis))
        challan.vehicleNumber.takeIf(String::isNotBlank)?.let { DeliveryChallanInfoRow(R.string.delivery_challan_vehicle_label, it) }
        challan.driverName.takeIf(String::isNotBlank)?.let { DeliveryChallanInfoRow(R.string.delivery_challan_driver_label, it) }
        challan.sourceInvoiceNumber?.takeIf(String::isNotBlank)?.let { DeliveryChallanInfoRow(R.string.delivery_challan_source_invoice_label, it) }
    }
}

@Composable
private fun DeliveryChallanInfoRow(label: Int, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.CalendarToday, null, modifier = Modifier.size(14.dp), tint = Color(0xFFFF6500))
        Text(
            stringResource(label),
            modifier = Modifier.padding(start = 8.dp).weight(1f),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun DeliveryChallanItemsTable(lines: List<EditableDeliveryChallanLine>) {
    val dark = isDeliveryChallanDark()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = DeliveryChallanCardShape,
        color = if (dark) Color(0xFF111A25) else Color.White,
        border = BorderStroke(1.dp, if (dark) Color(0xFF283646) else Color(0xFFEEE8E3)),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth().background(if (dark) Color(0xFF16212E) else Color(0xFFFFF8F4)).padding(horizontal = 10.dp, vertical = 6.dp),
            ) {
                Text(stringResource(R.string.delivery_challan_item), modifier = Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold)
                Text(stringResource(R.string.delivery_challan_quantity_header), modifier = Modifier.width(56.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End)
            }
            lines.forEachIndexed { index, line ->
                if (index > 0) HorizontalDivider(color = if (dark) Color(0xFF283646) else Color(0xFFF0E9E4))
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 7.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(line.description, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        line.materialId?.takeIf(String::isNotBlank)?.let {
                            Text(stringResource(R.string.delivery_challan_product_code, it), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Text(
                        listOfNotNull(line.quantity?.toPlainString(), line.unit.takeIf(String::isNotBlank)).joinToString(" "),
                        modifier = Modifier.width(56.dp),
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.End,
                    )
                }
            }
        }
    }
}

@Composable
private fun DeliveryChallanNotesCard(notes: String) {
    DeliveryChallanSurface {
        DetailsSectionTitle(stringResource(R.string.delivery_challan_notes))
        Text(notes, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun DeliveryChallanDetailsActions(
    canEdit: Boolean,
    canDispatch: Boolean,
    canCancel: Boolean,
    enabled: Boolean,
    onEvent: (DeliveryChallanDetailsUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (canEdit || canDispatch || canCancel) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canDispatch) DeliveryChallanActionButton(stringResource(R.string.delivery_challan_dispatch), Icons.Outlined.LocalShipping, { onEvent(DeliveryChallanDetailsUiEvent.Dispatch) }, Modifier.weight(1f), enabled, primary = true)
                if (canEdit) DeliveryChallanActionButton(stringResource(R.string.delivery_challan_edit), Icons.Outlined.Edit, { onEvent(DeliveryChallanDetailsUiEvent.Edit) }, Modifier.weight(1f), enabled)
                if (canCancel) DeliveryChallanActionButton(stringResource(R.string.cancel), Icons.Outlined.Cancel, { onEvent(DeliveryChallanDetailsUiEvent.Cancel) }, Modifier.weight(1f), enabled)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            DeliveryChallanActionButton(stringResource(R.string.delivery_challan_preview_pdf), Icons.Outlined.PictureAsPdf, { onEvent(DeliveryChallanDetailsUiEvent.PreviewPdf) }, Modifier.weight(1f), enabled)
            DeliveryChallanActionButton(stringResource(R.string.delivery_challan_share_pdf), Icons.Outlined.Share, { onEvent(DeliveryChallanDetailsUiEvent.SharePdf) }, Modifier.weight(1f), enabled)
        }
    }
}

@Composable
private fun DeliveryChallanActionButton(text: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier, enabled: Boolean, primary: Boolean = false) {
    val dark = isDeliveryChallanDark()
    Surface(
        modifier = modifier.clip(DeliveryChallanButtonShape).clickable(enabled = enabled, onClick = onClick),
        shape = DeliveryChallanButtonShape,
        color = if (primary) Color(0xFFFF6500) else if (dark) Color(0xFF111A25) else Color.White,
        border = if (primary) null else BorderStroke(1.dp, if (dark) Color(0xFF283646) else Color(0xFFEEE8E3)),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 9.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, modifier = Modifier.size(15.dp), tint = if (primary) Color.White else MaterialTheme.colorScheme.onSurface)
            Text(text, modifier = Modifier.padding(start = 5.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = if (primary) Color.White else MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun DeliveryChallanSurface(
    color: Color? = null,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val dark = isDeliveryChallanDark()
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = DeliveryChallanCardShape,
        color = color ?: if (dark) Color(0xFF111A25) else Color.White,
        border = BorderStroke(1.dp, if (dark) Color(0xFF283646) else Color(0xFFEEE8E3)),
        shadowElevation = if (dark) 0.dp else 2.dp,
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp), content = content)
    }
}

@Composable
private fun DetailsSectionTitle(text: String) = Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)

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

@Composable
private fun isDeliveryChallanDark(): Boolean = MaterialTheme.colorScheme.background.red < .2f

private val DeliveryChallanCardShape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
private val DeliveryChallanButtonShape = androidx.compose.foundation.shape.RoundedCornerShape(8.dp)

private fun formatDeliveryChallanDetailsDate(value: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US).format(Date(value))
