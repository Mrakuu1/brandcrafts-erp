package com.brandcrafts.erp.ui.components

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.filledTonalButtonColors(),
    ) {
        Text(text = text)
    }
}

@Preview(showBackground = true)
@Composable
private fun SecondaryButtonPreview() {
    BrandCraftsTheme { SecondaryButton(text = "View details", onClick = {}) }
}
