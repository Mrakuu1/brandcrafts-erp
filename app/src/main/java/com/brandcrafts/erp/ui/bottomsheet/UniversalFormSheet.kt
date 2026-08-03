package com.brandcrafts.erp.ui.bottomsheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import com.brandcrafts.erp.ui.theme.BrandSpacing
import com.brandcrafts.erp.ui.components.PrimaryButton
import com.brandcrafts.erp.ui.components.SecondaryButton

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
    containerColor: Color? = null,
    expanded: Boolean = false,
    peopleStyle: Boolean = false,
    content: @Composable () -> Unit,
) {
    BrandBottomSheet(
        title = title,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        containerColor = if (peopleStyle) containerColor ?: peopleFormSheetColor() else containerColor,
        fillAvailableHeight = expanded,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(if (expanded) Modifier.fillMaxHeight() else Modifier)
                .imePadding()
                .padding(top = BrandSpacing.Lg),
            verticalArrangement = Arrangement.spacedBy(BrandSpacing.Xl),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (expanded) Modifier.weight(1f) else Modifier.heightIn(max = 480.dp))
                    .verticalScroll(rememberScrollState()),
            ) {
                content()
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BrandSpacing.Md),
            ) {
                cancelActionLabel?.let { label ->
                    if (peopleStyle) {
                        PeopleFormSecondaryButton(
                            text = label,
                            onClick = onDismissRequest,
                            enabled = !primaryActionLoading,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        SecondaryButton(
                            text = label,
                            onClick = onDismissRequest,
                            enabled = !primaryActionLoading,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                if (peopleStyle) {
                    PeopleFormPrimaryButton(
                        text = primaryActionLabel,
                        onClick = onPrimaryAction,
                        enabled = primaryActionEnabled,
                        loading = primaryActionLoading,
                        modifier = Modifier.weight(1f),
                    )
                } else {
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
}

@Composable
private fun PeopleFormSecondaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier,
) {
    val shape = MaterialTheme.shapes.small
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(peopleFormFieldColor())
            .border(1.dp, peopleFormOutline(), shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun PeopleFormPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    loading: Boolean,
    modifier: Modifier,
) {
    val shape = MaterialTheme.shapes.small
    Box(
        modifier = modifier
            .height(48.dp)
            .shadow(4.dp, shape, clip = false)
            .clip(shape)
            .background(
                Brush.horizontalGradient(
                    listOf(Color(0xFFFF7A00), Color(0xFFFF4C00)),
                ),
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun peopleFormSheetColor(): Color =
    if (MaterialTheme.colorScheme.background.red < .2f) Color(0xFF111A25) else Color.White

@Composable
private fun peopleFormFieldColor(): Color =
    if (MaterialTheme.colorScheme.background.red < .2f) Color(0xFF16212E) else Color(0xFFFFFCFA)

@Composable
private fun peopleFormOutline(): Color =
    if (MaterialTheme.colorScheme.background.red < .2f) Color(0xFF283646) else Color(0xFFEEE8E3)

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
