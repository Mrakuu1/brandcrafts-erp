package com.brandcrafts.erp.feature.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.ui.bottomsheet.BrandBottomSheet

@Composable
internal fun PeopleFormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    errorMessage: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    keyboardOptions: androidx.compose.foundation.text.KeyboardOptions = androidx.compose.foundation.text.KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val border = when {
        errorMessage != null -> MaterialTheme.colorScheme.error
        focused -> MaterialTheme.colorScheme.primary
        else -> peopleFormOutline()
    }
    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth().height(if (singleLine) 60.dp else 108.dp),
            label = { Text(label, style = MaterialTheme.typography.bodyMedium) },
            singleLine = singleLine,
            enabled = enabled,
            readOnly = readOnly,
            interactionSource = source,
            keyboardOptions = keyboardOptions,
            visualTransformation = visualTransformation,
            shape = MaterialTheme.shapes.small,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = peopleFormFieldColor(),
                unfocusedContainerColor = peopleFormFieldColor(),
                disabledContainerColor = peopleFormFieldColor().copy(alpha = .6f),
                focusedBorderColor = border,
                unfocusedBorderColor = border,
                errorBorderColor = MaterialTheme.colorScheme.error,
            ),
        )
        if (errorMessage != null) {
            Text(
                errorMessage,
                modifier = Modifier.padding(start = 4.dp, top = 3.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
internal fun PeopleFormSheet(
    title: String,
    primaryActionLabel: String,
    onPrimaryAction: () -> Unit,
    onDismissRequest: () -> Unit,
    primaryActionLoading: Boolean = false,
    primaryActionEnabled: Boolean = true,
    cancelActionLabel: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    BrandBottomSheet(
        title = title,
        onDismissRequest = onDismissRequest,
        containerColor = peopleFormSheetColor(),
        fillAvailableHeight = true,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .imePadding()
                .padding(top = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(end = 1.dp)
                    .verticalScroll(rememberScrollState()),
            ) {
                content()
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PeopleSecondaryAction(
                    text = cancelActionLabel,
                    onClick = onDismissRequest,
                    enabled = !primaryActionLoading,
                    modifier = Modifier.weight(1f),
                )
                PeoplePrimaryAction(
                    text = primaryActionLabel,
                    onClick = onPrimaryAction,
                    enabled = primaryActionEnabled && !primaryActionLoading,
                    loading = primaryActionLoading,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun PeopleSecondaryAction(text: String, onClick: () -> Unit, enabled: Boolean, modifier: Modifier) {
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
        Text(text, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun PeoplePrimaryAction(text: String, onClick: () -> Unit, enabled: Boolean, loading: Boolean, modifier: Modifier) {
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
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.height(20.dp),
                color = Color.White,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text, style = MaterialTheme.typography.labelLarge, color = Color.White)
        }
    }
}

@Composable
private fun peopleFormSheetColor(): Color = if (peopleFormDark()) Color(0xFF111A25) else Color.White

@Composable
private fun peopleFormFieldColor(): Color = if (peopleFormDark()) Color(0xFF16212E) else Color(0xFFFFFCFA)

@Composable
private fun peopleFormOutline(): Color = if (peopleFormDark()) Color(0xFF283646) else Color(0xFFEEE8E3)

@Composable
private fun peopleFormDark(): Boolean = MaterialTheme.colorScheme.background.red < .2f

/** Keeps active switches visually consistent with the Inventory edit/create sheets. */
@Composable
internal fun peopleFormSwitchColors(): SwitchColors {
    val dark = peopleFormDark()
    return SwitchDefaults.colors(
        checkedThumbColor = Color.White,
        checkedTrackColor = MaterialTheme.colorScheme.primary,
        uncheckedThumbColor = if (dark) Color(0xFFAAB7C4) else Color(0xFF9C948C),
        uncheckedTrackColor = if (dark) Color(0xFF263543) else Color(0xFFE5E0DB),
        uncheckedBorderColor = if (dark) Color(0xFF344554) else Color(0xFFCFC7C0),
        disabledUncheckedThumbColor = if (dark) Color(0xFF687785) else Color(0xFFB5AEA7),
        disabledUncheckedTrackColor = if (dark) Color(0xFF1C2732) else Color(0xFFEDE9E5),
        disabledUncheckedBorderColor = if (dark) Color(0xFF2B3946) else Color(0xFFDCD6D1),
    )
}
