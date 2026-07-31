package com.brandcrafts.erp.ui.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.ui.components.PrimaryButton
import com.brandcrafts.erp.ui.components.SecondaryButton
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import com.brandcrafts.erp.ui.theme.BrandSpacing

@Composable
fun UniversalFormSheet(
    title: String,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    primaryActionLoading: Boolean = false,
    primaryActionEnabled: Boolean = true,
    cancelActionLabel: String? = null,
    content: @Composable () -> Unit,
) {
    BrandBottomSheet(
        title = title,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(top = BrandSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(BrandSpacing.Xl),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 480.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                content()
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BrandSpacing.Md),
            ) {
                cancelActionLabel?.let { label ->
                    SecondaryButton(
                        text = label,
                        onClick = onDismissRequest,
                        enabled = !primaryActionLoading,
                        modifier = Modifier.weight(1f),
                    )
                }
                PrimaryButton(
                    text = primaryActionLabel,
                    onClick = onPrimaryAction,
                    enabled = primaryActionEnabled,
                    loading = primaryActionLoading,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UniversalFormSheetPreview() {
    BrandCraftsTheme {
        UniversalFormSheet(
            title = "Add material",
            primaryActionLabel = "Save",
            cancelActionLabel = "Cancel",
            onPrimaryAction = {},
            onDismissRequest = {},
        ) {
            Text(text = "Form content")
        }
    }
}
