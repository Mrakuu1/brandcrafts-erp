package com.brandcrafts.erp.ui.components

import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import com.brandcrafts.erp.ui.theme.BrandSpacing
import com.brandcrafts.erp.ui.theme.BrandVisualTokens

@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier.defaultMinSize(minHeight = BrandSpacing.MinTouchTarget),
        enabled = enabled,
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = BrandVisualTokens.FieldSurfaceAlpha),
        ),
        elevation = ButtonDefaults.filledTonalButtonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(text = text)
    }
}

@Preview(showBackground = true)
@Composable
private fun SecondaryButtonPreview() {
    BrandCraftsTheme { SecondaryButton(text = "View details", onClick = {}) }
}
