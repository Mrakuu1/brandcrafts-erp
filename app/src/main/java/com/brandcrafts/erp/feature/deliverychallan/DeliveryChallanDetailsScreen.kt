package com.brandcrafts.erp.feature.deliverychallan

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
import androidx.compose.material3.CircularProgressIndicator
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
import com.brandcrafts.erp.domain.model.DeliveryChallanStatus
import com.brandcrafts.erp.ui.components.AppTopBar
import com.brandcrafts.erp.ui.components.EmptyState
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
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
            DeliveryChallanDetailsContent.Loaded -> {
                val challan = state.challan
                if (challan == null) {
                    EmptyState(
                        title = stringResource(R.string.delivery_challan_error),
                        description = stringResource(R.string.delivery_challan_error_description),
                        modifier = Modifier.padding(innerPadding),
                    )
                } else {
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
                }
            }
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
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            DeliveryChallanHeader(
                challan = challan,
                canEdit = canEdit,
                canDispatch = canDispatch,
                canCancel = canCancel,
                isOperating = isOperating,
                isGeneratingPdf = isGeneratingPdf,
                onEvent = onEvent,
            )
        }
        item {
            DeliveryChallanDetailsSection(
                title = stringResource(R.string.delivery_challan_customer),
                values = listOf(
                    stringResource(R.string.delivery_challan_customer_value, challan.customer.label),
                    stringResource(R.string.delivery_challan_delivery_address_value, challan.deliveryAddress),
                    stringResource(R.string.delivery_challan_date_value, formatDeliveryChallanDetailsDate(challan.dateMillis)),
                ),
            )
        }
        challan.sourceInvoiceNumber?.let { number ->
            item {
                DeliveryChallanDetailsSection(
                    title = stringResource(R.string.delivery_challan_source),
                    values = listOf(stringResource(R.string.delivery_challan_source_invoice, number)),
                )
            }
        }
        if (challan.vehicleNumber.isNotBlank() || challan.driverName.isNotBlank()) {
            item {
                DeliveryChallanDetailsSection(
                    title = stringResource(R.string.delivery_challan_dispatch_details),
                    values = buildList {
                        if (challan.vehicleNumber.isNotBlank()) {
                            add(stringResource(R.string.delivery_challan_vehicle_number_value, challan.vehicleNumber))
                        }
                        if (challan.driverName.isNotBlank()) {
                            add(stringResource(R.string.delivery_challan_driver_name_value, challan.driverName))
                        }
                    },
                )
            }
        }
        item { Text(stringResource(R.string.delivery_challan_items), style = MaterialTheme.typography.titleMedium) }
        items(items = challan.lines, key = EditableDeliveryChallanLine::localId) { line ->
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
                ListItem(
                    headlineContent = { Text(line.description) },
                    supportingContent = {
                        Text(
                            stringResource(
                                R.string.delivery_challan_quantity_value,
                                line.quantity?.toPlainString().orEmpty(),
                                line.unit,
                            ),
                        )
                    },
                )
            }
        }
        if (challan.notes.isNotBlank()) {
            item {
                DeliveryChallanDetailsSection(
                    title = stringResource(R.string.delivery_challan_notes),
                    values = listOf(challan.notes),
                )
            }
        }
    }
}

@Composable
private fun DeliveryChallanHeader(
    challan: DeliveryChallanDetailsModel,
    canEdit: Boolean,
    canDispatch: Boolean,
    canCancel: Boolean,
    isOperating: Boolean,
    isGeneratingPdf: Boolean,
    onEvent: (DeliveryChallanDetailsUiEvent) -> Unit,
) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(challan.number, style = MaterialTheme.typography.titleLarge)
                StatusChip(
                    label = stringResource(challan.status.labelRes()),
                    tone = challan.status.tone(),
                )
            }
            if (isOperating || isGeneratingPdf) {
                CircularProgressIndicator()
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (canEdit) {
                        TextButton(onClick = { onEvent(DeliveryChallanDetailsUiEvent.Edit) }) {
                            Text(stringResource(R.string.delivery_challan_edit))
                        }
                    }
                    if (canDispatch) {
                        TextButton(onClick = { onEvent(DeliveryChallanDetailsUiEvent.Dispatch) }) {
                            Text(stringResource(R.string.delivery_challan_dispatch))
                        }
                    }
                    if (canCancel) {
                        TextButton(onClick = { onEvent(DeliveryChallanDetailsUiEvent.Cancel) }) {
                            Text(stringResource(R.string.delivery_challan_cancel))
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { onEvent(DeliveryChallanDetailsUiEvent.PreviewPdf) },
                    enabled = !isGeneratingPdf,
                ) {
                    Text(stringResource(R.string.delivery_challan_preview_pdf))
                }
                TextButton(
                    onClick = { onEvent(DeliveryChallanDetailsUiEvent.SharePdf) },
                    enabled = !isGeneratingPdf,
                ) {
                    Text(stringResource(R.string.delivery_challan_share_pdf))
                }
            }
        }
    }
}

@Composable
private fun DeliveryChallanDetailsSection(title: String, values: List<String>) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            values.forEach { Text(it, style = MaterialTheme.typography.bodyMedium) }
        }
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

private fun formatDeliveryChallanDetailsDate(value: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US).format(Date(value))
