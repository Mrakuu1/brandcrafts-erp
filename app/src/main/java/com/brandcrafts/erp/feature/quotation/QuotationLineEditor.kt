package com.brandcrafts.erp.feature.quotation

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.format.formatIndianCurrency
import com.brandcrafts.erp.ui.components.AppTextField
import java.math.BigDecimal

@Composable
fun QuotationLineEditor(
    line: EditableQuotationLine,
    inventoryOptions: List<QuotationInventoryOption>,
    canRemove: Boolean,
    enabled: Boolean,
    onInventorySelected: (String) -> Unit,
    onLineChanged: (quantity: String?, unitPrice: String?, discount: String?, tax: String?) -> Unit,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = if (MaterialTheme.colorScheme.background.red < .2f) .75f else .55f),
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (MaterialTheme.colorScheme.background.red < .2f) {
                Color(0xFF16212E)
            } else {
                Color.White
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(stringResource(R.string.quotation_line_item), style = MaterialTheme.typography.titleMedium)
                IconButton(
                    onClick = onRemoveClick,
                    enabled = enabled && canRemove,
                ) {
                    Icon(Icons.Outlined.DeleteOutline, stringResource(R.string.quotation_remove_line))
                }
            }
            QuotationInventorySelector(
                selectedId = line.materialId,
                options = inventoryOptions,
                enabled = enabled,
                errorMessage = line.inventoryError?.toErrorText(),
                onSelected = onInventorySelected,
            )
            AppTextField(
                value = line.quantity,
                onValueChange = { onLineChanged(it, null, null, null) },
                label = stringResource(R.string.quotation_quantity),
                errorMessage = line.quantityError?.toErrorText(),
                enabled = enabled,
                keyboardOptions = decimalKeyboardOptions(),
            )
            AppTextField(
                value = line.unitPrice,
                onValueChange = { onLineChanged(null, it, null, null) },
                label = stringResource(R.string.quotation_unit_price),
                errorMessage = line.unitPriceError?.toErrorText(),
                enabled = enabled,
                keyboardOptions = decimalKeyboardOptions(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AppTextField(
                    value = line.discountPercent,
                    onValueChange = { onLineChanged(null, null, it, null) },
                    label = stringResource(R.string.quotation_discount_percent),
                    errorMessage = line.discountError?.toErrorText(),
                    enabled = enabled,
                    keyboardOptions = decimalKeyboardOptions(),
                    modifier = Modifier.weight(1f),
                )
                AppTextField(
                    value = line.taxPercent,
                    onValueChange = { onLineChanged(null, null, null, it) },
                    label = stringResource(R.string.quotation_tax_percent),
                    errorMessage = line.taxError?.toErrorText(),
                    enabled = enabled,
                    keyboardOptions = decimalKeyboardOptions(),
                    modifier = Modifier.weight(1f),
                )
            }
            line.total?.let {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    QuotationAmountRow(stringResource(R.string.quotation_line_subtotal), line.subtotal)
                    QuotationAmountRow(stringResource(R.string.quotation_line_discount), line.discount)
                    QuotationAmountRow(stringResource(R.string.quotation_line_tax), line.tax)
                    QuotationAmountRow(stringResource(R.string.quotation_line_total), it, emphasized = true)
                }
            }
        }
    }
}

@Composable
internal fun QuotationAmountRow(label: String, amount: BigDecimal?, emphasized: Boolean = false) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = if (emphasized) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium)
        Text(
            text = amount?.let(::formatQuotationCurrency).orEmpty(),
            style = if (emphasized) MaterialTheme.typography.titleSmall else MaterialTheme.typography.bodyMedium,
        )
    }
}

internal fun decimalKeyboardOptions() = androidx.compose.foundation.text.KeyboardOptions(keyboardType = KeyboardType.Decimal)
internal fun formatQuotationCurrency(value: BigDecimal): String = formatIndianCurrency(value)

@Composable
internal fun QuotationFieldError.toErrorText(): String = stringResource(
    when (this) {
        QuotationFieldError.REQUIRED -> R.string.quotation_field_required
        QuotationFieldError.MALFORMED -> R.string.quotation_field_malformed_number
        QuotationFieldError.OUT_OF_RANGE -> R.string.quotation_field_out_of_range
    },
)
