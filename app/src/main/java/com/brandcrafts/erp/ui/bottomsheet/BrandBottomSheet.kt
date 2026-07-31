package com.brandcrafts.erp.ui.bottomsheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.brandcrafts.erp.ui.theme.BrandSpacing
import com.brandcrafts.erp.ui.theme.BrandVisualTokens

/** Shared shell for lightweight selection and action flows. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandBottomSheet(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = BrandVisualTokens.OverlaySurfaceAlpha),
        contentColor = MaterialTheme.colorScheme.onSurface,
        tonalElevation = BrandVisualTokens.OverlayElevation,
        scrimColor = Color.Black.copy(alpha = 0.48f),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.outline.copy(alpha = .55f),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = BrandSpacing.Xl, vertical = BrandSpacing.Md),
        ) {
            Text(text = title, style = MaterialTheme.typography.headlineSmall)
            content()
        }
    }
}
