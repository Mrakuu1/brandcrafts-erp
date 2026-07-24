package com.brandcrafts.erp.feature.inventory

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.brandcrafts.erp.R
import com.brandcrafts.erp.ui.bottomsheet.UniversalFormSheet
import com.brandcrafts.erp.ui.components.AppTextField
import com.brandcrafts.erp.ui.components.ErrorState
import com.brandcrafts.erp.ui.components.LoadingView
import androidx.compose.ui.unit.dp

@Composable
fun StockInScreen(state: StockInUiState, onEvent: (StockInUiEvent) -> Unit) {
    when {
        state.isLoading -> LoadingView(message = stringResource(R.string.stock_in_loading))
        state.materialName.isBlank() -> ErrorState(
            title = stringResource(R.string.stock_in_error_title), description = stringResource(state.errorMessage ?: R.string.stock_in_error_material),
            retryLabel = stringResource(R.string.retry), onRetry = { onEvent(StockInUiEvent.RetryClicked) },
            secondaryActionLabel = stringResource(R.string.cancel), onSecondaryAction = { onEvent(StockInUiEvent.CancelClicked) },
        )
        else -> UniversalFormSheet(
            title = stringResource(R.string.stock_in_title), primaryActionLabel = stringResource(R.string.stock_in_save),
            onPrimaryAction = { onEvent(StockInUiEvent.SaveClicked) }, onDismissRequest = { onEvent(StockInUiEvent.CancelClicked) },
            primaryActionLoading = state.isSaving, cancelActionLabel = stringResource(R.string.cancel),
        ) {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                state.errorMessage?.let { Text(stringResource(it), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium) }
                AppTextField(value = state.materialName, onValueChange = {}, label = stringResource(R.string.stock_in_material), readOnly = true)
                AppTextField(value = state.quantity, onValueChange = { onEvent(StockInUiEvent.QuantityChanged(it)) }, label = stringResource(R.string.stock_in_quantity), errorMessage = state.quantityError?.let { stringResource(it) }, enabled = !state.isSaving, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                AppTextField(value = state.referenceId, onValueChange = { onEvent(StockInUiEvent.ReferenceChanged(it)) }, label = stringResource(R.string.stock_in_reference), enabled = !state.isSaving)
                AppTextField(value = state.remarks, onValueChange = { onEvent(StockInUiEvent.RemarksChanged(it)) }, label = stringResource(R.string.stock_in_remarks), enabled = !state.isSaving, singleLine = false)
            }
        }
    }
}
