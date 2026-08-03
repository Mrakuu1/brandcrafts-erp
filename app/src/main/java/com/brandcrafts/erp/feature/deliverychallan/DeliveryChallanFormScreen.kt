package com.brandcrafts.erp.feature.deliverychallan

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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

@Composable
fun DeliveryChallanFormScreen(
    state: DeliveryChallanFormUiState,
    onEvent: (DeliveryChallanFormUiEvent) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
    ) { innerPadding ->
        when (state.content) {
            DeliveryChallanFormContent.Loading -> LoadingView(
                message = stringResource(R.string.delivery_challan_form_loading),
                modifier = Modifier.padding(innerPadding),
            )
            DeliveryChallanFormContent.Error -> ErrorState(
                title = stringResource(R.string.delivery_challan_error),
                description = stringResource(R.string.delivery_challan_error_description),
                retryLabel = stringResource(R.string.retry),
                onRetry = { onEvent(DeliveryChallanFormUiEvent.Retry) },
                secondaryActionLabel = stringResource(R.string.cancel),
                onSecondaryAction = onBack,
                modifier = Modifier.padding(innerPadding),
            )
            DeliveryChallanFormContent.Ready -> UniversalFormSheet(
                title = stringResource(state.mode.titleRes()),
                primaryActionLabel = stringResource(state.mode.saveLabelRes()),
                onPrimaryAction = { onEvent(DeliveryChallanFormUiEvent.Save) },
                onDismissRequest = onBack,
                primaryActionLoading = state.isSaving,
                primaryActionEnabled = !state.isSaving,
                cancelActionLabel = stringResource(R.string.cancel),
                expanded = true,
                peopleStyle = true,
                modifier = Modifier.padding(innerPadding),
            ) {
                DeliveryChallanFormFields(state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun DeliveryChallanFormFields(
    state: DeliveryChallanFormUiState,
    onEvent: (DeliveryChallanFormUiEvent) -> Unit,
) {
    val enabled = !state.isSaving
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.mode == DeliveryChallanFormMode.INVOICE_CREATE) {
            Text(
                text = stringResource(
                    R.string.delivery_challan_imported_invoice,
                    state.sourceInvoiceNumber.orEmpty(),
                ),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        SectionHeader(title = stringResource(R.string.delivery_challan_customer))
        DeliveryChallanCustomerSelector(
            selectedId = state.selectedCustomerId,
            options = state.customerOptions,
            enabled = enabled && state.mode != DeliveryChallanFormMode.INVOICE_CREATE,
            errorMessage = state.errors.customer.message(),
            onSelected = { onEvent(DeliveryChallanFormUiEvent.CustomerChanged(it)) },
        )
        if (state.customerOptions.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.delivery_challan_no_customers),
                description = stringResource(R.string.delivery_challan_no_customers_description),
            )
        }
        AppTextField(
            value = state.deliveryAddress,
            onValueChange = { onEvent(DeliveryChallanFormUiEvent.AddressChanged(it)) },
            label = stringResource(R.string.delivery_challan_delivery_address),
            enabled = enabled,
            errorMessage = state.errors.address.message(),
            singleLine = false,
        )
        DeliveryChallanDateField(
            selectedDateMillis = state.dateMillis,
            enabled = enabled,
            errorMessage = state.errors.date.message(),
            onDateSelected = { onEvent(DeliveryChallanFormUiEvent.DateChanged(it)) },
        )
        AppTextField(
            value = state.vehicleNumber,
            onValueChange = { onEvent(DeliveryChallanFormUiEvent.VehicleChanged(it)) },
            label = stringResource(R.string.delivery_challan_vehicle_number),
            enabled = enabled,
        )
        AppTextField(
            value = state.driverName,
            onValueChange = { onEvent(DeliveryChallanFormUiEvent.DriverChanged(it)) },
            label = stringResource(R.string.delivery_challan_driver_name),
            enabled = enabled,
        )
        SectionHeader(title = stringResource(R.string.delivery_challan_items))
        state.lines.forEach { line ->
            key(line.localId) {
                DeliveryChallanLineEditor(
                    line = line,
                    materialOptions = state.materialOptions,
                    enabled = enabled,
                    isImported = state.mode == DeliveryChallanFormMode.INVOICE_CREATE,
                    onMaterialSelected = {
                        onEvent(DeliveryChallanFormUiEvent.MaterialChanged(line.localId, it))
                    },
                    onDescriptionChanged = {
                        onEvent(DeliveryChallanFormUiEvent.LineChanged(line.localId, description = it))
                    },
                    onQuantityChanged = {
                        onEvent(
                            DeliveryChallanFormUiEvent.LineChanged(
                                id = line.localId,
                                quantity = it.toDeliveryChallanDecimalOrNull(),
                            ),
                        )
                    },
                    onUnitChanged = {
                        onEvent(DeliveryChallanFormUiEvent.LineChanged(line.localId, unit = it))
                    },
                    onRemove = { onEvent(DeliveryChallanFormUiEvent.RemoveLine(line.localId)) },
                )
            }
        }
        if (state.materialOptions.isEmpty() && state.mode != DeliveryChallanFormMode.INVOICE_CREATE) {
            EmptyState(
                title = stringResource(R.string.delivery_challan_no_materials),
                description = stringResource(R.string.delivery_challan_no_materials_description),
            )
        }
        if (state.mode != DeliveryChallanFormMode.INVOICE_CREATE) {
            OutlinedButton(
                text = stringResource(R.string.delivery_challan_add_item),
                onClick = { onEvent(DeliveryChallanFormUiEvent.AddLine) },
                enabled = enabled,
            )
        }
        AppTextField(
            value = state.notes,
            onValueChange = { onEvent(DeliveryChallanFormUiEvent.NotesChanged(it)) },
            label = stringResource(R.string.delivery_challan_notes),
            enabled = enabled,
            singleLine = false,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeliveryChallanCustomerSelector(
    selectedId: String?,
    options: List<DeliveryChallanCustomerOption>,
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
            value = selected?.label.orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(stringResource(R.string.delivery_challan_select_customer)) },
            isError = errorMessage != null,
            supportingText = errorMessage?.let { value -> { Text(value) } },
            trailingIcon = { androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.small,
            colors = formOutlinedTextFieldColors(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = formDropdownMenuContainerColor(),
            tonalElevation = 0.dp,
        ) {
            options.forEach { customer ->
                DropdownMenuItem(
                    text = { Text(customer.label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    onClick = {
                        onSelected(customer.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun DeliveryChallanLineEditor(
    line: EditableDeliveryChallanLine,
    materialOptions: List<DeliveryChallanMaterialOption>,
    enabled: Boolean,
    isImported: Boolean,
    onMaterialSelected: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onUnitChanged: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (!isImported) {
            DeliveryChallanMaterialSelector(
                selectedId = line.materialId,
                options = materialOptions,
                enabled = enabled,
                errorMessage = line.errors.materialOrDescription.message(),
                onSelected = onMaterialSelected,
            )
        }
        AppTextField(
            value = line.description,
            onValueChange = onDescriptionChanged,
            label = stringResource(R.string.delivery_challan_description),
            enabled = enabled && !isImported,
            errorMessage = line.errors.materialOrDescription.message(),
        )
        AppTextField(
            value = line.quantity?.toPlainString().orEmpty(),
            onValueChange = onQuantityChanged,
            label = stringResource(R.string.delivery_challan_quantity),
            enabled = enabled,
            errorMessage = line.errors.quantity.message(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        AppTextField(
            value = line.unit,
            onValueChange = onUnitChanged,
            label = stringResource(R.string.delivery_challan_unit),
            enabled = enabled && !isImported,
            errorMessage = line.errors.unit.message(),
        )
        TextButton(onClick = onRemove, enabled = enabled && !isImported) {
            Text(stringResource(R.string.delivery_challan_remove_item))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeliveryChallanMaterialSelector(
    selectedId: String?,
    options: List<DeliveryChallanMaterialOption>,
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
            value = selected?.let {
                stringResource(R.string.delivery_challan_material_option, it.name, it.unit)
            }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(stringResource(R.string.delivery_challan_select_material)) },
            isError = errorMessage != null,
            supportingText = errorMessage?.let { value -> { Text(value) } },
            trailingIcon = { androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.small,
            colors = formOutlinedTextFieldColors(),
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = formDropdownMenuContainerColor(),
            tonalElevation = 0.dp,
        ) {
            options.forEach { material ->
                DropdownMenuItem(
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(material.name)
                            Text(material.unit, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    onClick = {
                        onSelected(material.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeliveryChallanDateField(
    selectedDateMillis: Long?,
    enabled: Boolean,
    errorMessage: String?,
    onDateSelected: (Long) -> Unit,
) {
    var showPicker by remember { mutableStateOf(false) }
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
    val label = stringResource(R.string.delivery_challan_date)
    OutlinedTextField(
        value = selectedDateMillis?.let(::formatDeliveryChallanFormDate).orEmpty(),
        onValueChange = {},
        readOnly = true,
        enabled = enabled,
        label = { Text(label) },
        isError = errorMessage != null,
        supportingText = errorMessage?.let { value -> { Text(value) } },
        trailingIcon = {
            IconButton(onClick = { showPicker = true }, enabled = enabled) {
                Icon(Icons.Outlined.CalendarToday, contentDescription = label)
            }
        },
        modifier = Modifier.fillMaxWidth().height(56.dp).clickable(enabled = enabled) { showPicker = true },
        shape = MaterialTheme.shapes.small,
        colors = formOutlinedTextFieldColors(),
    )
    if (showPicker) {
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let(onDateSelected)
                        showPicker = false
                    },
                ) { Text(stringResource(R.string.delivery_challan_select_date)) }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun DeliveryChallanFormMode.titleRes(): Int = when (this) {
    DeliveryChallanFormMode.INDEPENDENT_CREATE -> R.string.delivery_challan_create_title
    DeliveryChallanFormMode.INVOICE_CREATE -> R.string.delivery_challan_create_from_invoice_title
    DeliveryChallanFormMode.EDIT_DRAFT -> R.string.delivery_challan_edit_title
}

private fun DeliveryChallanFormMode.saveLabelRes(): Int = when (this) {
    DeliveryChallanFormMode.EDIT_DRAFT -> R.string.delivery_challan_update
    DeliveryChallanFormMode.INDEPENDENT_CREATE,
    DeliveryChallanFormMode.INVOICE_CREATE,
    -> R.string.delivery_challan_save
}

@Composable
private fun DeliveryChallanFieldError?.message(): String? = when (this) {
    DeliveryChallanFieldError.REQUIRED -> stringResource(R.string.delivery_challan_form_required)
    DeliveryChallanFieldError.INVALID_QUANTITY -> stringResource(R.string.delivery_challan_error_quantity)
    DeliveryChallanFieldError.INVALID_UNIT -> stringResource(R.string.delivery_challan_error_unit)
    DeliveryChallanFieldError.INVALID_DATE -> stringResource(R.string.delivery_challan_error_date_required)
    DeliveryChallanFieldError.INVALID_SOURCE -> stringResource(R.string.delivery_challan_error_invalid_source)
    null -> null
}

private fun String.toDeliveryChallanDecimalOrNull(): BigDecimal? =
    trim().takeIf(String::isNotEmpty)?.toBigDecimalOrNull()

private fun formatDeliveryChallanFormDate(value: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US).format(Date(value))
