package com.brandcrafts.erp.feature.inventory

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.ui.bottomsheet.BrandBottomSheet

@Composable
internal fun InventoryFormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val shape = MaterialTheme.shapes.small
    val border = when {
        errorMessage != null -> MaterialTheme.colorScheme.error
        focused -> MaterialTheme.colorScheme.primary
        else -> inventoryFormOutline()
    }
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(if (singleLine) 60.dp else 108.dp),
            // A floating label stays visible for existing values and while the
            // user is typing, unlike a placeholder-only field.
            label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
            singleLine = singleLine,
            enabled = enabled,
            readOnly = readOnly,
            interactionSource = source,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = keyboardType),
            shape = shape,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = inventoryFormFieldColor(),
                unfocusedContainerColor = inventoryFormFieldColor(),
                disabledContainerColor = inventoryFormFieldColor().copy(alpha = .6f),
                focusedBorderColor = border,
                unfocusedBorderColor = border,
                errorBorderColor = MaterialTheme.colorScheme.error,
            ),
        )
        if (errorMessage != null) {
            Text(errorMessage, modifier = Modifier.padding(start = 4.dp, top = 3.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
internal fun InventoryFormSheet(
    title: String,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    onDismissRequest: () -> Unit,
    primaryActionLoading: Boolean = false,
    primaryActionEnabled: Boolean = true,
    cancelActionLabel: String? = null,
    modifier: Modifier = Modifier,
    expanded: Boolean = false,
    content: @Composable () -> Unit,
) {
    BrandBottomSheet(
        title = title,
        onDismissRequest = onDismissRequest,
        containerColor = inventoryFormSheetColor(),
        fillAvailableHeight = expanded,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                // Add/Edit material has enough fields to need the full available sheet height.
                // The action row remains below the scrollable form.
                .then(if (expanded) Modifier.fillMaxHeight() else Modifier)
                // Keep the persistent action row above the software keyboard.
                .imePadding()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .then(if (expanded) Modifier.weight(1f) else Modifier.height(360.dp))
                    .padding(end = 1.dp)
                    .verticalScroll(rememberScrollState()),
            ) { content() }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                cancelActionLabel?.let { label ->
                    InventorySecondaryAction(label, onDismissRequest, !primaryActionLoading, Modifier.weight(1f))
                }
                InventoryPrimaryAction(primaryActionLabel, onPrimaryAction, primaryActionEnabled && !primaryActionLoading, primaryActionLoading, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun InventorySecondaryAction(text: String, onClick: () -> Unit, enabled: Boolean, modifier: Modifier) {
    val shape = MaterialTheme.shapes.small
    Box(
        modifier = modifier
            .height(48.dp)
            .clip(shape)
            .background(inventoryFormFieldColor())
            .border(1.dp, inventoryFormOutline(), shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) { Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface) }
}

@Composable
private fun InventoryPrimaryAction(text: String, onClick: () -> Unit, enabled: Boolean, loading: Boolean, modifier: Modifier) {
    val shape = MaterialTheme.shapes.small
    Box(
        modifier = modifier
            .height(48.dp)
            .shadow(4.dp, shape, clip = false)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(Color(0xFFFF7A00), Color(0xFFFF4C00))))
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = androidx.compose.ui.Alignment.Center,
    ) {
        if (loading) CircularProgressIndicator(modifier = Modifier.height(20.dp), color = Color.White, strokeWidth = 2.dp)
        else Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White)
    }
}

@Composable
internal fun inventoryFormSheetColor(): Color = if (inventoryFormDark()) Color(0xFF111A25) else Color.White

@Composable
private fun inventoryFormFieldColor(): Color = if (inventoryFormDark()) Color(0xFF16212E) else Color(0xFFFFFCFA)

@Composable
private fun inventoryFormOutline(): Color = if (inventoryFormDark()) Color(0xFF283646) else Color(0xFFEEE8E3)

@Composable
private fun inventoryFormDark(): Boolean = MaterialTheme.colorScheme.background.red < .2f
