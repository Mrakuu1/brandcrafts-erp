package com.brandcrafts.erp.ui.bottomsheet

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.ui.theme.BrandSpacing

/** Shared shell for lightweight selection and action flows. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrandBottomSheet(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color? = null,
    fillAvailableHeight: Boolean = false,
    header: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        sheetState = sheetState,
        shape = MaterialTheme.shapes.extraLarge,
        // Keep all sheets in the same clean surface family as Inventory and
        // document forms; a translucent theme surface caused a brown cast.
        containerColor = containerColor ?: if (MaterialTheme.colorScheme.background.red < .2f) {
            Color(0xFF111A25)
        } else {
            Color.White
        },
        contentColor = MaterialTheme.colorScheme.onSurface,
        // Material's tonal overlay tints dark sheets with the primary orange,
        // which reads as brown. The supplied container color is authoritative.
        tonalElevation = 0.dp,
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
                .then(if (fillAvailableHeight) Modifier.fillMaxHeight() else Modifier)
                .padding(horizontal = BrandSpacing.Xl, vertical = BrandSpacing.Md),
        ) {
            if (header != null) header() else Text(text = title, style = MaterialTheme.typography.headlineSmall)
            if (fillAvailableHeight) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                ) {
                    content()
                }
            } else {
                content()
            }
        }
    }
}
