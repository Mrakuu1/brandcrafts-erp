package com.brandcrafts.erp.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import com.brandcrafts.erp.ui.theme.BrandSpacing
import com.brandcrafts.erp.ui.theme.BrandVisualTokens

@Composable
fun AppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    errorMessage: String? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    singleLine: Boolean = true,
    leadingIcon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    trailingContent: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = BrandSpacing.MinTouchTarget),
        shape = MaterialTheme.shapes.medium,
        enabled = enabled,
        readOnly = readOnly,
        singleLine = singleLine,
        isError = errorMessage != null,
        label = { Text(text = label) },
        placeholder = placeholder?.let { text -> { Text(text = text) } },
        leadingIcon = leadingIcon?.let { icon ->
            { Icon(imageVector = icon, contentDescription = null) }
        },
        trailingIcon = trailingContent ?: trailingIcon?.let { icon ->
            { Icon(imageVector = icon, contentDescription = null) }
        },
        supportingText = errorMessage?.let { message -> { Text(text = message) } },
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = BrandVisualTokens.FieldSurfaceAlpha),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = BrandVisualTokens.FieldSurfaceAlpha),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f),
            errorContainerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = .36f),
            focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = .78f),
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = BrandVisualTokens.LightBorderAlpha),
            errorBorderColor = MaterialTheme.colorScheme.error,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun AppTextFieldPreview() {
    BrandCraftsTheme {
        AppTextField(
            value = "",
            onValueChange = {},
            label = "Material name",
            placeholder = "Enter a material name",
        )
    }
}
