package com.brandcrafts.erp.ui.components

import androidx.compose.material3.OutlinedButton as MaterialOutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import com.brandcrafts.erp.ui.theme.BrandSpacing
import com.brandcrafts.erp.ui.theme.BrandVisualTokens

@Composable
fun OutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    MaterialOutlinedButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = BrandSpacing.MinTouchTarget),
        enabled = enabled,
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = BrandVisualTokens.LightBorderAlpha)),
    ) {
        Text(text = text)
    }
}

@Preview(showBackground = true)
@Composable
private fun OutlinedButtonPreview() {
    BrandCraftsTheme { OutlinedButton(text = "Cancel", onClick = {}) }
}
