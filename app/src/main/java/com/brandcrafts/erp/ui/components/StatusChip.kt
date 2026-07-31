package com.brandcrafts.erp.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import com.brandcrafts.erp.ui.theme.BrandSpacing
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import com.brandcrafts.erp.ui.theme.BrandVisualTokens

enum class StatusTone { NEUTRAL, SUCCESS, WARNING, ERROR, INFO }

@Composable
fun StatusChip(
    label: String,
    modifier: Modifier = Modifier,
    tone: StatusTone = StatusTone.NEUTRAL,
) {
    val (containerColor, labelColor) = when (tone) {
        StatusTone.NEUTRAL -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        StatusTone.SUCCESS -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        StatusTone.WARNING -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        StatusTone.ERROR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        StatusTone.INFO -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
    }
    Surface(
        modifier = modifier.semantics { contentDescription = label },
        shape = MaterialTheme.shapes.small,
        color = containerColor.copy(alpha = BrandVisualTokens.FieldSurfaceAlpha),
        contentColor = labelColor,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = BrandSpacing.Sm, vertical = BrandSpacing.Xs),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusChipPreview() {
    BrandCraftsTheme { StatusChip(label = "Pending", tone = StatusTone.WARNING) }
}
