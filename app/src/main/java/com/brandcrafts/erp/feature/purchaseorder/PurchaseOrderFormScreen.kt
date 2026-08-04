package com.brandcrafts.erp.feature.purchaseorder

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.format.formatIndianCurrency
import com.brandcrafts.erp.ui.bottomsheet.UniversalFormSheet
import com.brandcrafts.erp.ui.components.AppTextField
import com.brandcrafts.erp.ui.components.formOutlinedTextFieldColors
import com.brandcrafts.erp.ui.components.formDropdownMenuContainerColor
import com.brandcrafts.erp.ui.components.EmptyState
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import com.brandcrafts.erp.ui.components.OutlinedButton
import com.brandcrafts.erp.ui.components.SectionHeader
import java.math.BigDecimal
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun PurchaseOrderFormScreen(
    state: PurchaseOrderFormUiState,
    onEvent: (PurchaseOrderFormUiEvent) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier,
        containerColor = Color.Transparent,
    ) { innerPadding ->
        when {
            state.loading -> LoadingView(
                message = stringResource(R.string.purchase_order_loading),
                modifier = Modifier.padding(innerPadding),
            )
            !state.editingAllowed -> ErrorState(
                title = stringResource(R.string.purchase_order_error),
                description = stringResource(R.string.purchase_order_error_draft_only_edit),
                retryLabel = stringResource(R.string.cancel),
                onRetry = onBack,
                modifier = Modifier.padding(innerPadding),
            )
            state.error && state.suppliers.isEmpty() && state.inventory.isEmpty() -> ErrorState(
                title = stringResource(R.string.purchase_order_error),
                description = stringResource(R.string.purchase_order_error_description),
                retryLabel = stringResource(R.string.retry),
                onRetry = { onEvent(PurchaseOrderFormUiEvent.Retry) },
                secondaryActionLabel = stringResource(R.string.cancel),
                onSecondaryAction = onBack,
                modifier = Modifier.padding(innerPadding),
            )
            else -> UniversalFormSheet(
                title = stringResource(state.mode.titleRes()),
                primaryActionLabel = stringResource(state.mode.actionRes()),
                onPrimaryAction = { onEvent(PurchaseOrderFormUiEvent.Save) },
                onDismissRequest = onBack,
                primaryActionLoading = state.saving,
                primaryActionEnabled = !state.loading && !state.saving && state.editingAllowed,
                cancelActionLabel = stringResource(R.string.cancel),
                expanded = true,
                peopleStyle = true,
                modifier = Modifier.padding(innerPadding),
            ) {
                PurchaseOrderFormFields(state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun PurchaseOrderFormFields(
    state: PurchaseOrderFormUiState,
    onEvent: (PurchaseOrderFormUiEvent) -> Unit,
) {
    val enabled = !state.saving && state.editingAllowed
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        state.number?.let { number ->
            Text(stringResource(R.string.purchase_order_number_label, number), style = MaterialTheme.typography.bodyMedium)
        }
        if (state.error) {
            Text(
                text = stringResource(R.string.purchase_order_error_generic),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        SectionHeader(title = stringResource(R.string.purchase_order_supplier_label))
        PurchaseOrderSupplierSelector(
            selectedId = state.supplierId,
            options = state.suppliers,
            enabled = enabled,
            errorMessage = state.supplierError?.message(),
            onSelected = { onEvent(PurchaseOrderFormUiEvent.SupplierSelected(it)) },
        )
        if (state.suppliers.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.purchase_order_form_no_suppliers),
                description = stringResource(R.string.purchase_order_form_no_suppliers_description),
            )
        }
        SectionHeader(title = stringResource(R.string.purchase_order_date_label))
        PurchaseOrderDateField(
            label = stringResource(R.string.purchase_order_date_label),
            selectedDateMillis = state.dateMillis,
            enabled = enabled,
            errorMessage = state.dateError?.message(),
            onDateSelected = { onEvent(PurchaseOrderFormUiEvent.DateSelected(it)) },
        )
        PurchaseOrderDateField(
            label = stringResource(R.string.purchase_order_expected_delivery_date_label),
            selectedDateMillis = state.expectedDeliveryDateMillis,
            enabled = enabled,
            errorMessage = null,
            onDateSelected = { onEvent(PurchaseOrderFormUiEvent.ExpectedDeliverySelected(it)) },
            onClear = { onEvent(PurchaseOrderFormUiEvent.ExpectedDeliveryCleared) },
        )
        SectionHeader(title = stringResource(R.string.purchase_order_items_title))
        state.lines.forEach { line ->
            key(line.localId) {
                PurchaseOrderLineEditor(
                    line = line,
                    inventory = state.inventory,
                    enabled = enabled,
                    onInventorySelected = { onEvent(PurchaseOrderFormUiEvent.InventorySelected(line.localId, it)) },
                    onDescriptionChanged = { onEvent(PurchaseOrderFormUiEvent.DescriptionChanged(line.localId, it)) },
                    onQuantityChanged = { onEvent(PurchaseOrderFormUiEvent.QuantityChanged(line.localId, it)) },
                    onUnitChanged = { onEvent(PurchaseOrderFormUiEvent.UnitChanged(line.localId, it)) },
                    onUnitPriceChanged = { onEvent(PurchaseOrderFormUiEvent.UnitPriceChanged(line.localId, it)) },
                    onRemove = { onEvent(PurchaseOrderFormUiEvent.RemoveLine(line.localId)) },
                )
            }
        }
        if (state.inventory.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.purchase_order_form_no_inventory),
                description = stringResource(R.string.purchase_order_form_no_inventory_description),
            )
        }
        OutlinedButton(
            text = stringResource(R.string.purchase_order_add_item),
            onClick = { onEvent(PurchaseOrderFormUiEvent.AddLine) },
            enabled = enabled,
        )
        state.total?.let { total -> PurchaseOrderTotals(total) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseOrderSupplierSelector(
    selectedId: String?,
    options: List<PurchaseOrderSupplierOption>,
    enabled: Boolean,
    errorMessage: String?,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.id == selectedId }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selected?.let { supplierLabel(it) }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(stringResource(R.string.purchase_order_select_supplier)) },
            isError = errorMessage != null,
            supportingText = errorMessage?.let { message -> { Text(message) } },
            trailingIcon = { androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth().height(60.dp),
            shape = MaterialTheme.shapes.small,
            colors = formOutlinedTextFieldColors(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = formDropdownMenuContainerColor(), tonalElevation = 0.dp) {
            options.forEach { supplier ->
                DropdownMenuItem(
                    text = { Text(supplierLabel(supplier), maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    onClick = { onSelected(supplier.id); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun PurchaseOrderLineEditor(
    line: EditablePurchaseOrderLine,
    inventory: List<PurchaseOrderInventoryOption>,
    enabled: Boolean,
    onInventorySelected: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onUnitChanged: (String) -> Unit,
    onUnitPriceChanged: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = .65f), MaterialTheme.shapes.medium)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        PurchaseOrderInventorySelector(
            selectedId = line.materialId,
            options = inventory,
            enabled = enabled,
            errorMessage = line.descriptionError?.message(),
            onSelected = onInventorySelected,
        )
        AppTextField(
            value = line.description,
            onValueChange = onDescriptionChanged,
            label = stringResource(R.string.purchase_order_description_label),
            enabled = enabled,
            errorMessage = line.descriptionError?.message(),
        )
        AppTextField(
            value = line.quantity,
            onValueChange = onQuantityChanged,
            label = stringResource(R.string.purchase_order_quantity_label),
            enabled = enabled,
            errorMessage = line.quantityError?.message(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        AppTextField(
            value = line.unit,
            onValueChange = onUnitChanged,
            label = stringResource(R.string.purchase_order_unit_label),
            enabled = enabled,
            errorMessage = line.unitError?.message(),
        )
        AppTextField(
            value = line.unitPrice,
            onValueChange = onUnitPriceChanged,
            label = stringResource(R.string.purchase_order_unit_price_label),
            enabled = enabled,
            errorMessage = line.unitPriceError?.message(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        line.lineTotal?.let { total ->
            Text(
                text = stringResource(R.string.purchase_order_line_total_label, formatCurrency(total)),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        TextButton(onClick = onRemove, enabled = enabled) {
            Text(stringResource(R.string.purchase_order_remove_item))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseOrderInventorySelector(
    selectedId: String?,
    options: List<PurchaseOrderInventoryOption>,
    enabled: Boolean,
    errorMessage: String?,
    onSelected: (String) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.id == selectedId }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selected?.let { item ->
                stringResource(R.string.purchase_order_inventory_option, item.name, item.unit)
            }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(stringResource(R.string.purchase_order_select_inventory_item)) },
            isError = errorMessage != null,
            supportingText = errorMessage?.let { message -> { Text(message) } },
            trailingIcon = { androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth().height(60.dp),
            shape = MaterialTheme.shapes.small,
            colors = formOutlinedTextFieldColors(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = formDropdownMenuContainerColor(), tonalElevation = 0.dp) {
            options.forEach { item ->
                DropdownMenuItem(
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(item.name)
                            Text(item.unit, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    onClick = { onSelected(item.id); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PurchaseOrderDateField(
    label: String,
    selectedDateMillis: Long?,
    enabled: Boolean,
    errorMessage: String?,
    onDateSelected: (Long) -> Unit,
    onClear: (() -> Unit)? = null,
) {
    var showingPicker by remember { mutableStateOf(false) }
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
    OutlinedTextField(
        value = selectedDateMillis?.let(::formatDate).orEmpty(),
        onValueChange = {},
        readOnly = true,
        enabled = enabled,
        label = { Text(label) },
        isError = errorMessage != null,
        supportingText = errorMessage?.let { message -> { Text(message) } },
        trailingIcon = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (onClear != null && selectedDateMillis != null) {
                    TextButton(onClick = onClear, enabled = enabled) {
                        Text(stringResource(R.string.purchase_order_clear_date))
                    }
                }
                androidx.compose.material3.IconButton(onClick = { showingPicker = true }, enabled = enabled) {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = stringResource(
                            if (onClear == null) R.string.purchase_order_select_date else R.string.purchase_order_select_expected_delivery_date,
                        ),
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth().height(60.dp).clickable(enabled = enabled) { showingPicker = true },
        shape = MaterialTheme.shapes.small,
        colors = formOutlinedTextFieldColors(),
    )
    if (showingPicker) {
        DatePickerDialog(
            onDismissRequest = { showingPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let(onDateSelected)
                        showingPicker = false
                    },
                ) { Text(stringResource(R.string.purchase_order_select_date)) }
            },
            dismissButton = {
                TextButton(onClick = { showingPicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun PurchaseOrderTotals(total: BigDecimal) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = stringResource(R.string.purchase_order_grand_total_label))
        Text(
            text = formatCurrency(total),
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

private fun PurchaseOrderFormMode.titleRes(): Int = when (this) {
    PurchaseOrderFormMode.Create -> R.string.purchase_order_create_title
    is PurchaseOrderFormMode.Edit -> R.string.purchase_order_edit_title
}

private fun PurchaseOrderFormMode.actionRes(): Int = when (this) {
    PurchaseOrderFormMode.Create -> R.string.purchase_order_save
    is PurchaseOrderFormMode.Edit -> R.string.purchase_order_update
}

@Composable
private fun PurchaseOrderFieldError.message(): String = stringResource(
    when (this) {
        PurchaseOrderFieldError.REQUIRED -> R.string.purchase_order_form_required
        PurchaseOrderFieldError.MALFORMED,
        PurchaseOrderFieldError.OUT_OF_RANGE -> R.string.purchase_order_form_invalid
        PurchaseOrderFieldError.LIMIT_EXCEEDED -> R.string.purchase_order_form_line_limit
    },
)

private fun supplierLabel(supplier: PurchaseOrderSupplierOption): String =
    supplier.company.ifBlank { supplier.name }

private fun formatDate(value: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US).format(Date(value))

private fun formatCurrency(value: BigDecimal): String =
    formatIndianCurrency(value)
