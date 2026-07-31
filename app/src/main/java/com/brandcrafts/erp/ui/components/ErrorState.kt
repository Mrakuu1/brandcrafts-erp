package com.brandcrafts.erp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import com.brandcrafts.erp.ui.theme.BrandSpacing

@Composable
fun ErrorState(
    title: String,
    description: String,
    retryLabel: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(BrandSpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.error,
            )
        }
        Text(text = title, style = MaterialTheme.typography.titleLarge)
        Text(text = description, style = MaterialTheme.typography.bodyMedium)
        PrimaryButton(text = retryLabel, onClick = onRetry)
        if (secondaryActionLabel != null && onSecondaryAction != null) {
            SecondaryButton(text = secondaryActionLabel, onClick = onSecondaryAction)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ErrorStatePreview() {
    BrandCraftsTheme {
        ErrorState(
            title = "Unable to load materials",
            description = "Check your connection and try again.",
            retryLabel = "Retry",
            onRetry = {},
        )
    }
}
