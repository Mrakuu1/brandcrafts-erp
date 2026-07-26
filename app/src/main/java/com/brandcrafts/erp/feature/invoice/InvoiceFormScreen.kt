package com.brandcrafts.erp.feature.invoice

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.ui.bottomsheet.UniversalFormSheet
import com.brandcrafts.erp.ui.components.AppTextField
import com.brandcrafts.erp.ui.components.EmptyState
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import com.brandcrafts.erp.ui.components.OutlinedButton
import com.brandcrafts.erp.ui.components.SectionHeader
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import java.math.BigDecimal
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale

@Composable
fun InvoiceFormScreen(
    state: InvoiceFormUiState,
    onEvent: (InvoiceFormUiEvent) -> Unit,
    onBack: () -> Unit,
    snackbarHostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        modifier = modifier,
    ) { innerPadding ->
        when {
            state.isLoading -> LoadingView(
                message = stringResource(R.string.invoice_loading),
                modifier = Modifier.padding(innerPadding),
            )
            state.isEditingBlocked -> ErrorState(
                title = stringResource(R.string.invoice_error),
                description = stringResource(R.string.invoice_error_draft_only_edit),
                retryLabel = stringResource(R.string.cancel),
                onRetry = onBack,
                modifier = Modifier.padding(innerPadding),
            )
            state.formError == InvoiceFormError.LOAD && state.mode is InvoiceFormMode.EditDraft -> ErrorState(
                title = stringResource(R.string.invoice_error),
                description = stringResource(R.string.invoice_error_description),
                retryLabel = stringResource(R.string.retry),
                onRetry = { onEvent(InvoiceFormUiEvent.Retry) },
                secondaryActionLabel = stringResource(R.string.cancel),
                onSecondaryAction = onBack,
                modifier = Modifier.padding(innerPadding),
            )
            state.formError == InvoiceFormError.LOAD &&
                state.customerOptions.isEmpty() && state.materialOptions.isEmpty() -> ErrorState(
                title = stringResource(R.string.invoice_error),
                description = stringResource(R.string.invoice_error_description),
                retryLabel = stringResource(R.string.retry),
                onRetry = { onEvent(InvoiceFormUiEvent.Retry) },
                secondaryActionLabel = stringResource(R.string.cancel),
                onSecondaryAction = onBack,
                modifier = Modifier.padding(innerPadding),
            )
            else -> UniversalFormSheet(
                title = stringResource(state.mode.titleRes()),
                primaryActionLabel = stringResource(state.mode.actionRes()),
                onPrimaryAction = { onEvent(InvoiceFormUiEvent.Save) },
                onDismissRequest = onBack,
                primaryActionLoading = state.isSaving,
                primaryActionEnabled = !state.isSaving && !state.isEditingBlocked,
                cancelActionLabel = stringResource(R.string.cancel),
                modifier = Modifier.padding(innerPadding),
            ) {
                InvoiceFormFields(state = state, onEvent = onEvent)
            }
        }
    }
}

@Composable
private fun InvoiceFormFields(
    state: InvoiceFormUiState,
    onEvent: (InvoiceFormUiEvent) -> Unit,
) {
    val enabled = !state.isSaving && !state.isEditingBlocked
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        state.invoiceNumber?.let { invoiceNumber ->
            Text(
                text = stringResource(R.string.invoice_number_label, invoiceNumber),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (state.formError == InvoiceFormError.SAVE) {
            Text(
                text = stringResource(R.string.invoice_error_generic),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        if (state.formError == InvoiceFormError.LOAD) {
            Text(
                text = stringResource(R.string.invoice_error_retry),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
            OutlinedButton(
                text = stringResource(R.string.retry),
                onClick = { onEvent(InvoiceFormUiEvent.Retry) },
                enabled = enabled,
            )
        }
        SectionHeader(title = stringResource(R.string.invoice_customer))
        InvoiceCustomerSelector(
            selectedId = state.customerId,
            options = state.customerOptions,
            enabled = enabled,
            errorMessage = state.errors.customer?.message(),
            onSelected = { onEvent(InvoiceFormUiEvent.CustomerSelected(it)) },
        )
        if (state.customerOptions.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.invoice_form_no_customers),
                description = stringResource(R.string.invoice_form_no_customers_description),
            )
        }
        SectionHeader(title = stringResource(R.string.invoice_date))
        InvoiceDateField(
            label = stringResource(R.string.invoice_date),
            selectedDateMillis = state.invoiceDateMillis,
            enabled = enabled,
            errorMessage = state.errors.invoiceDate?.message(),
            onDateSelected = { onEvent(InvoiceFormUiEvent.InvoiceDateChanged(it)) },
        )
        InvoiceDateField(
            label = stringResource(R.string.invoice_due_date),
            selectedDateMillis = state.dueDateMillis,
            enabled = enabled,
            errorMessage = state.errors.dueDate?.message(),
            onDateSelected = { onEvent(InvoiceFormUiEvent.DueDateChanged(it)) },
            onClear = { onEvent(InvoiceFormUiEvent.DueDateChanged(null)) },
        )
        SectionHeader(title = stringResource(R.string.invoice_items))
        state.errors.items?.let { error ->
            Text(
                text = error.message(),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        state.lines.forEach { line ->
            key(line.localId) {
                InvoiceLineEditor(
                    line = line,
                    materialOptions = state.materialOptions,
                    enabled = enabled,
                    onMaterialSelected = { onEvent(InvoiceFormUiEvent.MaterialSelected(line.localId, it)) },
                    onDescriptionChanged = {
                        onEvent(InvoiceFormUiEvent.LineChanged(line.localId, description = it))
                    },
                    onQuantityChanged = {
                        onEvent(InvoiceFormUiEvent.LineChanged(line.localId, quantity = it.toDecimalOrNull()))
                    },
                    onUnitChanged = { onEvent(InvoiceFormUiEvent.LineChanged(line.localId, unit = it)) },
                    onUnitPriceChanged = {
                        onEvent(InvoiceFormUiEvent.LineChanged(line.localId, unitPrice = it.toDecimalOrNull()))
                    },
                    onDiscountChanged = {
                        onEvent(InvoiceFormUiEvent.LineChanged(line.localId, discountPercent = it.toDecimalOrNull()))
                    },
                    onTaxChanged = {
                        onEvent(InvoiceFormUiEvent.LineChanged(line.localId, taxPercent = it.toDecimalOrNull()))
                    },
                    onRemove = { onEvent(InvoiceFormUiEvent.RemoveLine(line.localId)) },
                )
            }
        }
        if (state.materialOptions.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.invoice_form_no_inventory),
                description = stringResource(R.string.invoice_form_no_inventory_description),
            )
        }
        OutlinedButton(
            text = stringResource(R.string.invoice_add_item),
            onClick = { onEvent(InvoiceFormUiEvent.AddLine) },
            enabled = enabled,
        )
        state.totals?.let { totals -> InvoiceTotalsSection(totals) }
        AppTextField(
            value = state.remarks,
            onValueChange = { onEvent(InvoiceFormUiEvent.RemarksChanged(it)) },
            label = stringResource(R.string.invoice_remarks),
            enabled = enabled,
            singleLine = false,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoiceCustomerSelector(
    selectedId: String?,
    options: List<InvoiceCustomerOption>,
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
            label = { Text(stringResource(R.string.invoice_select_customer)) },
            isError = errorMessage != null,
            supportingText = errorMessage?.let { message -> { Text(message) } },
            trailingIcon = { androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { customer ->
                DropdownMenuItem(
                    text = { Text(customer.label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    onClick = { onSelected(customer.id); expanded = false },
                )
            }
        }
    }
}

@Composable
private fun InvoiceLineEditor(
    line: EditableInvoiceLine,
    materialOptions: List<InvoiceMaterialOption>,
    enabled: Boolean,
    onMaterialSelected: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onQuantityChanged: (String) -> Unit,
    onUnitChanged: (String) -> Unit,
    onUnitPriceChanged: (String) -> Unit,
    onDiscountChanged: (String) -> Unit,
    onTaxChanged: (String) -> Unit,
    onRemove: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        InvoiceMaterialSelector(
            selectedId = line.materialId,
            options = materialOptions,
            enabled = enabled,
            errorMessage = line.errors.material?.message(),
            onSelected = onMaterialSelected,
        )
        AppTextField(
            value = line.description,
            onValueChange = onDescriptionChanged,
            label = stringResource(R.string.invoice_description),
            enabled = enabled,
            errorMessage = line.errors.description?.message(),
        )
        AppTextField(
            value = line.quantity?.toPlainString().orEmpty(),
            onValueChange = onQuantityChanged,
            label = stringResource(R.string.invoice_quantity),
            enabled = enabled,
            errorMessage = line.errors.quantity?.message(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        AppTextField(
            value = line.unit,
            onValueChange = onUnitChanged,
            label = stringResource(R.string.invoice_unit),
            enabled = enabled,
            errorMessage = line.errors.unit?.message(),
        )
        AppTextField(
            value = line.unitPrice?.toPlainString().orEmpty(),
            onValueChange = onUnitPriceChanged,
            label = stringResource(R.string.invoice_unit_price),
            enabled = enabled,
            errorMessage = line.errors.unitPrice?.message(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        AppTextField(
            value = line.discountPercent?.toPlainString().orEmpty(),
            onValueChange = onDiscountChanged,
            label = stringResource(R.string.invoice_discount_percent),
            enabled = enabled,
            errorMessage = line.errors.discountPercent?.message(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        AppTextField(
            value = line.taxPercent?.toPlainString().orEmpty(),
            onValueChange = onTaxChanged,
            label = stringResource(R.string.invoice_tax_percent),
            enabled = enabled,
            errorMessage = line.errors.taxPercent?.message(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        )
        line.lineTotal?.let { total ->
            Text(
                text = stringResource(R.string.invoice_line_total, formatInvoiceCurrency(total)),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        TextButton(onClick = onRemove, enabled = enabled) {
            Text(stringResource(R.string.invoice_remove_item))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoiceMaterialSelector(
    selectedId: String?,
    options: List<InvoiceMaterialOption>,
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
            value = selected?.let { material ->
                stringResource(R.string.invoice_material_option, material.name, material.unit)
            }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(stringResource(R.string.invoice_select_material)) },
            isError = errorMessage != null,
            supportingText = errorMessage?.let { message -> { Text(message) } },
            trailingIcon = { androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { material ->
                DropdownMenuItem(
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(material.name)
                            Text(material.unit, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    onClick = { onSelected(material.id); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvoiceDateField(
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
        value = selectedDateMillis?.let(::formatInvoiceDate).orEmpty(),
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
                        Text(stringResource(R.string.invoice_clear_due_date))
                    }
                }
                IconButton(onClick = { showingPicker = true }, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = label,
                    )
                }
            }
        },
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled) { showingPicker = true },
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
                ) { Text(stringResource(R.string.invoice_select_date)) }
            },
            dismissButton = {
                TextButton(onClick = { showingPicker = false }) { Text(stringResource(R.string.cancel)) }
            },
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun InvoiceTotalsSection(totals: InvoiceFormTotals) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        SectionHeader(title = stringResource(R.string.invoice_totals))
        InvoiceTotalRow(R.string.invoice_subtotal, totals.subtotal)
        InvoiceTotalRow(R.string.invoice_discount_total, totals.discountTotal)
        InvoiceTotalRow(R.string.invoice_tax_total, totals.taxTotal)
        InvoiceTotalRow(R.string.invoice_grand_total, totals.grandTotal, emphasized = true)
    }
}

@Composable
private fun InvoiceTotalRow(labelRes: Int, amount: BigDecimal, emphasized: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = stringResource(labelRes),
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
        )
        Text(
            text = formatInvoiceCurrency(amount),
            style = if (emphasized) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
        )
    }
}

private fun InvoiceFormMode.titleRes(): Int = when (this) {
    InvoiceFormMode.Create -> R.string.invoice_create
    is InvoiceFormMode.EditDraft -> R.string.invoice_edit
}

private fun InvoiceFormMode.actionRes(): Int = when (this) {
    InvoiceFormMode.Create -> R.string.invoice_save
    is InvoiceFormMode.EditDraft -> R.string.invoice_update
}

@Composable
private fun InvoiceFieldError.message(): String = stringResource(
    when (this) {
        InvoiceFieldError.REQUIRED -> R.string.invoice_form_required
        InvoiceFieldError.INVALID_DATE -> R.string.invoice_error_date_required
        InvoiceFieldError.DUE_DATE_BEFORE_INVOICE_DATE -> R.string.invoice_error_due_date_before_invoice_date
        InvoiceFieldError.INVALID_QUANTITY -> R.string.invoice_error_invalid_quantity
        InvoiceFieldError.INVALID_UNIT -> R.string.invoice_error_invalid_unit
        InvoiceFieldError.INVALID_UNIT_PRICE -> R.string.invoice_error_invalid_unit_price
        InvoiceFieldError.INVALID_DECIMAL,
        InvoiceFieldError.OUT_OF_RANGE -> R.string.invoice_form_invalid
    },
)

private fun String.toDecimalOrNull(): BigDecimal? = trim().takeIf(String::isNotEmpty)?.toBigDecimalOrNull()

private fun formatInvoiceDate(value: Long): String =
    DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US).format(Date(value))

private fun formatInvoiceCurrency(value: BigDecimal): String =
    NumberFormat.getCurrencyInstance(Locale.US).format(value)

@Preview(showBackground = true)
@Composable
private fun InvoiceCreateFormPreview() {
    BrandCraftsTheme {
        InvoiceFormScreen(
            state = previewInvoiceFormState(),
            onEvent = {},
            onBack = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun InvoiceEditFormPreview() {
    BrandCraftsTheme(darkTheme = true) {
        InvoiceFormScreen(
            state = previewInvoiceFormState().copy(
                mode = InvoiceFormMode.EditDraft("invoice-1"),
                invoiceNumber = "INV-000001",
            ),
            onEvent = {},
            onBack = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InvoiceFormLoadingPreview() {
    BrandCraftsTheme {
        InvoiceFormScreen(
            state = InvoiceFormUiState(mode = InvoiceFormMode.Create, isLoading = true),
            onEvent = {},
            onBack = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InvoiceFormBlockedPreview() {
    BrandCraftsTheme {
        InvoiceFormScreen(
            state = InvoiceFormUiState(
                mode = InvoiceFormMode.EditDraft("invoice-1"),
                isEditingBlocked = true,
            ),
            onEvent = {},
            onBack = {},
            snackbarHostState = SnackbarHostState(),
        )
    }
}

private fun previewInvoiceFormState(): InvoiceFormUiState {
    val line = EditableInvoiceLine(
        localId = "preview-line",
        persistedLineId = null,
        materialId = "material-1",
        description = "Premium print material",
        quantity = BigDecimal("2"),
        unit = "piece",
        unitPrice = BigDecimal("1250.00"),
        discountPercent = BigDecimal("10"),
        taxPercent = BigDecimal("18"),
        lineSubtotal = BigDecimal("2500.00"),
        lineDiscount = BigDecimal("250.00"),
        taxableAmount = BigDecimal("2250.00"),
        lineTax = BigDecimal("405.00"),
        lineTotal = BigDecimal("2655.00"),
    )
    return InvoiceFormUiState(
        mode = InvoiceFormMode.Create,
        customerOptions = listOf(InvoiceCustomerOption("customer-1", "Acme Design Studio")),
        materialOptions = listOf(InvoiceMaterialOption("material-1", "Premium print material", "piece")),
        customerId = "customer-1",
        invoiceDateMillis = 1_784_073_600_000L,
        dueDateMillis = 1_786_665_600_000L,
        lines = listOf(line),
        totals = InvoiceFormTotals(
            subtotal = BigDecimal("2500.00"),
            discountTotal = BigDecimal("250.00"),
            taxTotal = BigDecimal("405.00"),
            grandTotal = BigDecimal("2655.00"),
        ),
    )
}
