package com.brandcrafts.erp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.background
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme
import com.brandcrafts.erp.ui.theme.BrandSpacing
import com.brandcrafts.erp.ui.theme.BrandMotion
import com.brandcrafts.erp.ui.theme.BrandVisualTokens

@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val shape = MaterialTheme.shapes.medium
    val scale by animateFloatAsState(
        if (pressed) BrandVisualTokens.ButtonPressedScale else 1f,
        BrandMotion.fast(),
        label = "buttonScale",
    )
    Button(
        onClick = onClick,
        modifier = modifier
            .defaultMinSize(minHeight = BrandSpacing.MinTouchTarget)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .background(
                brush = Brush.horizontalGradient(
                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = .84f)),
                ),
                shape = shape,
            ),
        enabled = enabled && !loading,
        colors = ButtonDefaults.buttonColors(
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = BrandVisualTokens.ButtonElevation,
            pressedElevation = BrandVisualTokens.ButtonPressedElevation,
        ),
        shape = shape,
        interactionSource = interactionSource,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(BrandSpacing.Sm)) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier.defaultMinSize(20.dp, 20.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp,
                )
            }
            Text(text = text)
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PrimaryButtonPreview() {
    BrandCraftsTheme { PrimaryButton(text = "Save changes", onClick = {}) }
}
