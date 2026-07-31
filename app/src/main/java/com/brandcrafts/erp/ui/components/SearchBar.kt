package com.brandcrafts.erp.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import com.brandcrafts.erp.ui.theme.BrandSpacing
import com.brandcrafts.erp.ui.theme.BrandVisualTokens

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    searchIcon: ImageVector? = Icons.Outlined.Search,
    searchIconContentDescription: String? = null,
    clearContentDescription: String? = null,
) {
    TextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth().defaultMinSize(minHeight = BrandSpacing.MinTouchTarget),
        shape = MaterialTheme.shapes.medium,
        enabled = enabled,
        singleLine = true,
        placeholder = { androidx.compose.material3.Text(text = placeholder) },
        leadingIcon = searchIcon?.let { icon ->
            { Icon(imageVector = icon, contentDescription = searchIconContentDescription) }
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = clearContentDescription)
                }
            }
        } else {
            null
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = BrandVisualTokens.FieldSurfaceAlpha),
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = BrandVisualTokens.FieldSurfaceAlpha),
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .45f),
            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
        ),
    )
}

@Preview(showBackground = true)
@Composable
private fun SearchBarPreview() {
    BrandCraftsTheme {
        SearchBar(query = "", onQueryChange = {}, placeholder = "Search materials")
    }
}
