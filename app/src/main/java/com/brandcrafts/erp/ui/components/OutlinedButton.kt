package com.brandcrafts.erp.ui.components

import androidx.compose.material3.OutlinedButton as MaterialOutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme

@Composable
fun OutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    MaterialOutlinedButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
    ) {
        Text(text = text)
    }
}

@Preview(showBackground = true)
@Composable
private fun OutlinedButtonPreview() {
    BrandCraftsTheme { OutlinedButton(text = "Cancel", onClick = {}) }
}
