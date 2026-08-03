package com.brandcrafts.erp.feature.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import com.brandcrafts.erp.R
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import androidx.compose.foundation.text.KeyboardOptions

@Composable
fun InventoryFormScreen(
    uiState: InventoryFormUiState,
    onEvent: (InventoryFormUiEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isInitialLoading -> LoadingView(
            message = stringResource(R.string.inventory_form_loading),
            modifier = modifier,
        )
        uiState.loadError != null -> ErrorState(
            title = stringResource(R.string.inventory_form_load_error_title),
            description = stringResource(uiState.loadError.descriptionRes()),
            retryLabel = stringResource(R.string.retry),
            onRetry = { onEvent(InventoryFormUiEvent.RetryLoadClicked) },
            secondaryActionLabel = stringResource(R.string.cancel),
            onSecondaryAction = { onEvent(InventoryFormUiEvent.CancelClicked) },
            modifier = modifier,
        )
        else -> InventoryFormSheet(
            title = stringResource(uiState.mode.titleRes()),
            primaryActionLabel = stringResource(uiState.mode.saveLabelRes()),
            onPrimaryAction = { onEvent(InventoryFormUiEvent.SaveClicked) },
            onDismissRequest = { onEvent(InventoryFormUiEvent.CancelClicked) },
            primaryActionLoading = uiState.isSaving,
            primaryActionEnabled = !uiState.isSaving,
            cancelActionLabel = stringResource(R.string.cancel),
            modifier = modifier,
            expanded = true,
        ) {
            InventoryFormFields(uiState = uiState, onEvent = onEvent)
        }
    }
}

@Composable
private fun InventoryFormFields(
    uiState: InventoryFormUiState,
    onEvent: (InventoryFormUiEvent) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        uiState.saveError?.let { error ->
            Text(
                text = stringResource(error.descriptionRes()),
                modifier = Modifier.padding(bottom = 4.dp),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        InventoryFormTextField(
            value = uiState.name,
            onValueChange = { onEvent(InventoryFormUiEvent.NameChanged(it)) },
            label = stringResource(R.string.inventory_form_name_label),
            errorMessage = uiState.errors.name?.let { stringResource(it) },
            enabled = !uiState.isSaving,
        )
        InventoryFormTextField(
            value = uiState.sku,
            onValueChange = { onEvent(InventoryFormUiEvent.SkuChanged(it)) },
            label = stringResource(R.string.inventory_form_sku_label),
            errorMessage = uiState.errors.sku?.let { stringResource(it) },
            enabled = !uiState.isSaving,
        )
        InventoryFormTextField(
            value = uiState.category,
            onValueChange = { onEvent(InventoryFormUiEvent.CategoryChanged(it)) },
            label = stringResource(R.string.inventory_form_category_label),
            errorMessage = uiState.errors.category?.let { stringResource(it) },
            enabled = !uiState.isSaving,
        )
        InventoryFormTextField(
            value = uiState.unit,
            onValueChange = { onEvent(InventoryFormUiEvent.UnitChanged(it)) },
            label = stringResource(R.string.inventory_form_unit_label),
            errorMessage = uiState.errors.unit?.let { stringResource(it) },
            enabled = !uiState.isSaving,
        )
        InventoryNumericField(
            value = uiState.availableQuantity,
            onValueChange = { onEvent(InventoryFormUiEvent.AvailableQuantityChanged(it)) },
            label = stringResource(R.string.inventory_form_available_quantity_label),
            errorMessage = uiState.errors.availableQuantity?.let { stringResource(it) },
            enabled = !uiState.isSaving,
        )
        InventoryNumericField(
            value = uiState.minimumQuantity,
            onValueChange = { onEvent(InventoryFormUiEvent.MinimumQuantityChanged(it)) },
            label = stringResource(R.string.inventory_form_minimum_quantity_label),
            errorMessage = uiState.errors.minimumQuantity?.let { stringResource(it) },
            enabled = !uiState.isSaving,
        )
        InventoryNumericField(
            value = uiState.purchasePrice,
            onValueChange = { onEvent(InventoryFormUiEvent.PurchasePriceChanged(it)) },
            label = stringResource(R.string.inventory_form_purchase_rate_label),
            errorMessage = uiState.errors.purchasePrice?.let { stringResource(it) },
            enabled = !uiState.isSaving,
        )
        InventoryNumericField(
            value = uiState.sellingPrice,
            onValueChange = { onEvent(InventoryFormUiEvent.SellingPriceChanged(it)) },
            label = stringResource(R.string.inventory_form_selling_rate_label),
            errorMessage = uiState.errors.sellingPrice?.let { stringResource(it) },
            enabled = !uiState.isSaving,
        )
        InventoryFormTextField(
            value = uiState.description,
            onValueChange = { onEvent(InventoryFormUiEvent.DescriptionChanged(it)) },
            label = stringResource(R.string.inventory_form_description_label),
            enabled = !uiState.isSaving,
            singleLine = false,
        )
        androidx.compose.foundation.layout.Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = stringResource(R.string.inventory_form_active_label),
                style = MaterialTheme.typography.bodyLarge,
            )
            Switch(
                checked = uiState.active,
                onCheckedChange = { onEvent(InventoryFormUiEvent.ActiveChanged(it)) },
                enabled = !uiState.isSaving,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color.White,
                    checkedTrackColor = MaterialTheme.colorScheme.primary,
                    uncheckedThumbColor = if (MaterialTheme.colorScheme.background.red < .2f) Color(0xFFAAB7C4) else Color(0xFF9C948C),
                    uncheckedTrackColor = if (MaterialTheme.colorScheme.background.red < .2f) Color(0xFF263543) else Color(0xFFE5E0DB),
                    uncheckedBorderColor = if (MaterialTheme.colorScheme.background.red < .2f) Color(0xFF344554) else Color(0xFFCFC7C0),
                    disabledUncheckedThumbColor = if (MaterialTheme.colorScheme.background.red < .2f) Color(0xFF687785) else Color(0xFFB5AEA7),
                    disabledUncheckedTrackColor = if (MaterialTheme.colorScheme.background.red < .2f) Color(0xFF1C2732) else Color(0xFFEDE9E5),
                    disabledUncheckedBorderColor = if (MaterialTheme.colorScheme.background.red < .2f) Color(0xFF2B3946) else Color(0xFFDCD6D1),
                ),
            )
        }
    }
}

@Composable
private fun InventoryNumericField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    errorMessage: String?,
    enabled: Boolean,
) {
    InventoryFormTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        errorMessage = errorMessage,
        enabled = enabled,
        keyboardType = KeyboardType.Decimal,
    )
}

private fun InventoryFormMode.titleRes(): Int = when (this) {
    InventoryFormMode.CREATE -> R.string.inventory_form_create_title
    InventoryFormMode.EDIT -> R.string.inventory_form_edit_title
}

private fun InventoryFormMode.saveLabelRes(): Int = when (this) {
    InventoryFormMode.CREATE -> R.string.inventory_form_create_action
    InventoryFormMode.EDIT -> R.string.inventory_form_save_action
}

private fun InventoryFormErrorType.descriptionRes(): Int = when (this) {
    InventoryFormErrorType.NETWORK -> R.string.inventory_form_error_network
    InventoryFormErrorType.UNAUTHORIZED -> R.string.inventory_form_error_unauthorized
    InventoryFormErrorType.DUPLICATE_SKU -> R.string.inventory_form_error_duplicate_sku
    InventoryFormErrorType.NOT_FOUND -> R.string.inventory_form_error_not_found
    InventoryFormErrorType.UNKNOWN -> R.string.inventory_form_error_unknown
}

@Preview(showBackground = true)
@Composable
private fun CreateInventoryFormPreview() {
    BrandCraftsTheme { InventoryFormScreen(previewCreateFormState(), onEvent = {}) }
}

@Preview(showBackground = true)
@Composable
private fun EditInventoryFormPreview() {
    BrandCraftsTheme { InventoryFormScreen(previewCreateFormState().copy(mode = InventoryFormMode.EDIT, itemId = "material-preview"), onEvent = {}) }
}

@Preview(showBackground = true)
@Composable
private fun InventoryFormValidationPreview() {
    BrandCraftsTheme {
        InventoryFormScreen(
            previewCreateFormState().copy(errors = InventoryFormFieldErrors(name = R.string.inventory_form_name_required)),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun InventoryFormLoadingPreview() {
    BrandCraftsTheme { InventoryFormScreen(previewCreateFormState().copy(isSaving = true), onEvent = {}) }
}

private fun previewCreateFormState() = InventoryFormUiState(
    mode = InventoryFormMode.CREATE,
    name = "Blue Vinyl",
    sku = "VIN-BLU-01",
    category = "Vinyl",
    unit = "rolls",
    availableQuantity = "12",
    minimumQuantity = "4",
    purchasePrice = "140",
    sellingPrice = "220",
)
