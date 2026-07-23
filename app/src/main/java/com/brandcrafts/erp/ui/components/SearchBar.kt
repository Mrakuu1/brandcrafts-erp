package com.brandcrafts.erp.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    searchIcon: ImageVector? = null,
    searchIconContentDescription: String? = null,
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        singleLine = true,
        placeholder = { androidx.compose.material3.Text(text = placeholder) },
        leadingIcon = searchIcon?.let { icon ->
            { Icon(imageVector = icon, contentDescription = searchIconContentDescription) }
        },
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        colors = OutlinedTextFieldDefaults.colors(),
    )
}

@Preview(showBackground = true)
@Composable
private fun SearchBarPreview() {
    BrandCraftsTheme {
        SearchBar(query = "", onQueryChange = {}, placeholder = "Search materials")
    }
}
