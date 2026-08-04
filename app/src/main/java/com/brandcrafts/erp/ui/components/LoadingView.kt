package com.brandcrafts.erp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import com.brandcrafts.erp.ui.theme.BrandSpacing

@Composable
fun LoadingView(
    modifier: Modifier = Modifier,
    message: String? = null,
) {
    Column(
        modifier = modifier.fillMaxSize().padding(BrandSpacing.Xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
    ) {
        CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
        message?.let { Text(text = it, style = MaterialTheme.typography.bodyMedium) }
    }
}

/** Blocks duplicate actions while keeping the current screen visible underneath. */
@Composable
fun CenteredLoadingOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    if (visible) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(BrandSpacing.Xl),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(36.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 3.dp,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoadingViewPreview() {
    BrandCraftsTheme { LoadingView(message = "Loading materials") }
}
