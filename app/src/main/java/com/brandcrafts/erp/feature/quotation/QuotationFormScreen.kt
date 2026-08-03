package com.brandcrafts.erp.feature.quotation

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
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.domain.model.QuotationStatus
import com.brandcrafts.erp.ui.bottomsheet.UniversalFormSheet
import com.brandcrafts.erp.ui.components.AppTextField
import com.brandcrafts.erp.ui.components.formOutlinedTextFieldColors
import com.brandcrafts.erp.ui.components.formDropdownMenuContainerColor
import com.brandcrafts.erp.ui.components.EmptyState
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import com.brandcrafts.erp.ui.components.OutlinedButton
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import java.math.BigDecimal
import java.text.DateFormat
import java.util.Date

@Composable
fun QuotationFormScreen(
    state: QuotationFormUiState,
    onEvent: (QuotationFormUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.loading -> LoadingView(message = stringResource(R.string.quotation_form_loading), modifier = modifier)
        state.error == QuotationFormError.LOAD -> ErrorState(
            title = stringResource(R.string.quotation_form_load_error_title),
            description = stringResource(R.string.quotation_form_load_error_description),
            retryLabel = stringResource(R.string.retry),
            onRetry = { onEvent(QuotationFormUiEvent.Retry) },
            secondaryActionLabel = stringResource(R.string.cancel),
            onSecondaryAction = { onEvent(QuotationFormUiEvent.Back) },
            modifier = modifier,
        )
        state.error == QuotationFormError.UNAUTHORIZED -> ErrorState(
            title = stringResource(R.string.quotation_form_unauthorized_title),
            description = stringResource(R.string.quotation_form_unauthorized_description),
            retryLabel = stringResource(R.string.quotation_form_back),
            onRetry = { onEvent(QuotationFormUiEvent.Back) },
            modifier = modifier,
        )
        state.blocked || state.error == QuotationFormError.NON_DRAFT -> ErrorState(
            title = stringResource(R.string.quotation_form_edit_unavailable_title),
            description = stringResource(R.string.quotation_form_edit_unavailable_description),
            retryLabel = stringResource(R.string.quotation_form_back),
            onRetry = { onEvent(QuotationFormUiEvent.Back) },
            modifier = modifier,
        )
        else -> UniversalFormSheet(
            title = stringResource(state.mode.titleRes()),
            primaryActionLabel = stringResource(state.mode.actionRes()),
            onPrimaryAction = { onEvent(QuotationFormUiEvent.Save) },
            onDismissRequest = { onEvent(QuotationFormUiEvent.Back) },
            primaryActionLoading = state.saving,
            primaryActionEnabled = state.saveEnabled,
            cancelActionLabel = stringResource(R.string.cancel),
            expanded = true,
            peopleStyle = true,
            modifier = modifier,
        ) {
            QuotationFormFields(state, onEvent)
        }
    }
}

@Composable
private fun QuotationFormFields(state: QuotationFormUiState, onEvent: (QuotationFormUiEvent) -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (state.error == QuotationFormError.SAVE || state.error == QuotationFormError.VALIDATION) {
            Text(
                text = stringResource(state.error.descriptionRes()),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        QuotationCustomerSelector(
            selectedId = state.customerId,
            options = state.customerOptions,
            enabled = !state.saving,
            errorMessage = state.customerError?.toErrorText(),
            onSelected = { onEvent(QuotationFormUiEvent.CustomerSelected(it)) },
        )
        if (state.customerOptions.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.quotation_form_no_customers),
                description = stringResource(R.string.quotation_form_no_customers_description),
            )
        }
        QuotationDateSelector(
            selectedDateMillis = state.validUntilMillis,
            enabled = !state.saving,
            errorMessage = state.validUntilError?.toErrorText(),
            onDateSelected = { onEvent(QuotationFormUiEvent.ValidUntilChanged(it)) },
        )
        Text(stringResource(R.string.quotation_lines), style = MaterialTheme.typography.titleMedium)
        state.lines.forEach { line -> key(line.localId) {
            QuotationLineEditor(
                    line = line,
                    inventoryOptions = state.inventoryOptions,
                    canRemove = state.lines.size > 1,
                    enabled = !state.saving,
                    onInventorySelected = { materialId -> onEvent(QuotationFormUiEvent.InventorySelected(line.localId, materialId)) },
                    onLineChanged = { quantity, unitPrice, discount, tax ->
                        onEvent(QuotationFormUiEvent.LineChanged(line.localId, quantity, unitPrice, discount, tax))
                    },
                    onRemoveClick = { onEvent(QuotationFormUiEvent.RemoveLine(line.localId)) },
                )
            }
        }
        if (state.inventoryOptions.isEmpty()) {
            EmptyState(
                title = stringResource(R.string.quotation_form_no_inventory),
                description = stringResource(R.string.quotation_form_no_inventory_description),
            )
        }
        OutlinedButton(
            text = stringResource(R.string.quotation_add_line),
            onClick = { onEvent(QuotationFormUiEvent.AddLine) },
            enabled = !state.saving && state.inventoryOptions.isNotEmpty(),
        )
        AppTextField(
            value = state.notes,
            onValueChange = { onEvent(QuotationFormUiEvent.NotesChanged(it)) },
            label = stringResource(R.string.quotation_notes),
            enabled = !state.saving,
            singleLine = false,
        )
        state.totals?.let { QuotationTotalsSummary(it) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotationCustomerSelector(
    selectedId: String?,
    options: List<QuotationCustomerOption>,
    enabled: Boolean,
    errorMessage: String?,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val label = options.firstOrNull { it.id == selectedId }?.label.orEmpty()
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = label,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(stringResource(R.string.quotation_customer)) },
            isError = errorMessage != null,
            supportingText = errorMessage?.let { { Text(it) } },
            trailingIcon = { androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.small,
            colors = formOutlinedTextFieldColors(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = formDropdownMenuContainerColor(), tonalElevation = 0.dp) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option.label, maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    onClick = { onSelected(option.id); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuotationInventorySelector(
    selectedId: String?,
    options: List<QuotationInventoryOption>,
    enabled: Boolean,
    errorMessage: String?,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val selected = options.firstOrNull { it.id == selectedId }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
        modifier = modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selected?.let { "${it.name} · ${it.unit}" }.orEmpty(),
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            label = { Text(stringResource(R.string.quotation_inventory_item)) },
            isError = errorMessage != null,
            supportingText = errorMessage?.let { { Text(it) } },
            trailingIcon = { androidx.compose.material3.ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier.menuAnchor().fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.small,
            colors = formOutlinedTextFieldColors(),
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }, containerColor = formDropdownMenuContainerColor(), tonalElevation = 0.dp) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(option.name)
                            Text(option.unit, style = MaterialTheme.typography.bodySmall)
                        }
                    },
                    onClick = { onSelected(option.id); expanded = false },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuotationDateSelector(
    selectedDateMillis: Long?,
    enabled: Boolean,
    errorMessage: String?,
    onDateSelected: (Long?) -> Unit,
) {
    var showingPicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
    OutlinedTextField(
        value = selectedDateMillis?.let(::formatQuotationDate).orEmpty(),
        onValueChange = {},
        readOnly = true,
        enabled = enabled,
        label = { Text(stringResource(R.string.quotation_valid_until_label)) },
        isError = errorMessage != null,
        supportingText = errorMessage?.let { { Text(it) } },
        trailingIcon = { androidx.compose.material3.IconButton(onClick = { showingPicker = true }, enabled = enabled) { androidx.compose.material3.Icon(Icons.Outlined.CalendarToday, stringResource(R.string.quotation_select_valid_until)) } },
        modifier = Modifier.fillMaxWidth().height(56.dp).clickable(enabled = enabled) { showingPicker = true },
        shape = MaterialTheme.shapes.small,
        colors = formOutlinedTextFieldColors(),
    )
    if (showingPicker) {
        DatePickerDialog(
            onDismissRequest = { showingPicker = false },
            confirmButton = { androidx.compose.material3.TextButton(onClick = { onDateSelected(datePickerState.selectedDateMillis); showingPicker = false }) { Text(stringResource(R.string.quotation_date_confirm)) } },
            dismissButton = { androidx.compose.material3.TextButton(onClick = { showingPicker = false }) { Text(stringResource(R.string.cancel)) } },
        ) { DatePicker(state = datePickerState) }
    }
}

@Composable
private fun QuotationTotalsSummary(totals: com.brandcrafts.erp.domain.usecase.quotation.QuotationTotals) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(stringResource(R.string.quotation_totals), style = MaterialTheme.typography.titleMedium)
        QuotationAmountRow(stringResource(R.string.quotation_subtotal), totals.subtotal)
        QuotationAmountRow(stringResource(R.string.quotation_discount_total), totals.discount)
        QuotationAmountRow(stringResource(R.string.quotation_taxable_total), totals.taxable)
        QuotationAmountRow(stringResource(R.string.quotation_tax_total), totals.tax)
        QuotationAmountRow(stringResource(R.string.quotation_grand_total), totals.grandTotal, emphasized = true)
    }
}

private fun QuotationFormMode.titleRes() = when (this) {
    QuotationFormMode.CREATE -> R.string.quotation_form_create_title
    QuotationFormMode.EDIT -> R.string.quotation_form_edit_title
}

private fun QuotationFormMode.actionRes() = when (this) {
    QuotationFormMode.CREATE -> R.string.quotation_form_create_action
    QuotationFormMode.EDIT -> R.string.quotation_form_save_action
}

private fun QuotationFormError.descriptionRes() = when (this) {
    QuotationFormError.VALIDATION -> R.string.quotation_form_validation_error
    QuotationFormError.SAVE -> R.string.quotation_form_save_error
    QuotationFormError.UNAUTHORIZED -> R.string.quotation_form_unauthorized_description
    QuotationFormError.NON_DRAFT -> R.string.quotation_form_edit_unavailable_description
    QuotationFormError.LOAD -> R.string.quotation_form_load_error_description
}

private fun formatQuotationDate(value: Long): String = DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(value))

@Preview(showBackground = true)
@Composable
private fun CreateQuotationFormPreview() { BrandCraftsTheme { QuotationFormScreen(previewQuotationForm(), {}) } }

@Preview(showBackground = true)
@Composable
private fun EditQuotationFormPreview() { BrandCraftsTheme { QuotationFormScreen(previewQuotationForm().copy(mode = QuotationFormMode.EDIT, quotationId = "preview", loadedStatus = QuotationStatus.DRAFT), {}) } }

@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun QuotationFormDarkPreview() { BrandCraftsTheme(darkTheme = true) { QuotationFormScreen(previewQuotationForm(), {}) } }

@Preview(showBackground = true)
@Composable
private fun QuotationFormErrorPreview() { BrandCraftsTheme { QuotationFormScreen(previewQuotationForm().copy(error = QuotationFormError.VALIDATION, customerError = QuotationFieldError.REQUIRED), {}) } }

@Preview(showBackground = true)
@Composable
private fun QuotationFormSavingPreview() { BrandCraftsTheme { QuotationFormScreen(previewQuotationForm().copy(saving = true), {}) } }

@Preview(showBackground = true)
@Composable
private fun QuotationFormBlockedPreview() { BrandCraftsTheme { QuotationFormScreen(previewQuotationForm().copy(blocked = true, error = QuotationFormError.NON_DRAFT), {}) } }

@Preview(showBackground = true)
@Composable
private fun QuotationFormLoadingPreview() { BrandCraftsTheme { QuotationFormScreen(QuotationFormUiState(), {}) } }

@Preview(showBackground = true)
@Composable
private fun QuotationFormEmptyCustomersPreview() { BrandCraftsTheme { QuotationFormScreen(previewQuotationForm().copy(customerOptions = emptyList()), {}) } }

@Preview(showBackground = true)
@Composable
private fun QuotationFormEmptyInventoryPreview() { BrandCraftsTheme { QuotationFormScreen(previewQuotationForm().copy(inventoryOptions = emptyList()), {}) } }

@Preview(showBackground = true)
@Composable
private fun QuotationFormUnauthorizedPreview() { BrandCraftsTheme { QuotationFormScreen(previewQuotationForm().copy(error = QuotationFormError.UNAUTHORIZED), {}) } }

@Preview(showBackground = true)
@Composable
private fun QuotationFormMultipleLinesPreview() {
    BrandCraftsTheme {
        val state = previewQuotationForm()
        QuotationFormScreen(state.copy(lines = state.lines + state.lines.single().copy(localId = "second-line")), {})
    }
}

private fun previewQuotationForm(): QuotationFormUiState {
    val line = EditableQuotationLine(materialId = "material", description = "Premium board", unit = "Sheet", quantity = "2", unitPrice = "100", discountPercent = "10", taxPercent = "18", subtotal = BigDecimal("200.00"), discount = BigDecimal("20.00"), taxable = BigDecimal("180.00"), tax = BigDecimal("32.40"), total = BigDecimal("212.40"))
    return QuotationFormUiState(loading = false, customerOptions = listOf(QuotationCustomerOption("customer", "Acme · Acme Studio")), inventoryOptions = listOf(QuotationInventoryOption("material", "Premium board", "Sheet")), customerId = "customer", validUntilMillis = 1_800_000_000_000, lines = listOf(line), totals = com.brandcrafts.erp.domain.usecase.quotation.QuotationTotals(BigDecimal("200.00"), BigDecimal("20.00"), BigDecimal("180.00"), BigDecimal("32.40"), BigDecimal("212.40")))
}
