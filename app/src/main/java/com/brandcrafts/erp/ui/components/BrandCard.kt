package com.brandcrafts.erp.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.remember
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.brandcrafts.erp.ui.theme.BrandSpacing
import com.brandcrafts.erp.ui.theme.BrandMotion
import com.brandcrafts.erp.ui.theme.BrandVisualTokens

/** Standard surface for grouped business information. */
@Composable
fun BrandCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(BrandSpacing.Lg),
    content: @Composable () -> Unit,
) {
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = BrandVisualTokens.CardSurfaceAlpha),
    )
    val border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = BrandVisualTokens.LightBorderAlpha))
    val elevation = CardDefaults.cardElevation(
        defaultElevation = BrandVisualTokens.CardElevation,
        pressedElevation = BrandVisualTokens.PressedCardElevation,
    )
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (pressed && onClick != null) BrandVisualTokens.PressedScale else 1f,
        BrandMotion.fast(),
        label = "cardScale",
    )

    if (onClick == null) {
        Card(
            modifier = modifier,
            shape = MaterialTheme.shapes.medium,
            colors = colors,
            border = border,
            elevation = elevation,
            content = { Column(Modifier.padding(contentPadding)) { content() } },
        )
    } else {
        Card(
            onClick = onClick,
            modifier = modifier.graphicsLayer { scaleX = scale; scaleY = scale },
            interactionSource = interactionSource,
            shape = MaterialTheme.shapes.medium,
            colors = colors,
            border = border,
            elevation = elevation,
            content = { Column(Modifier.padding(contentPadding)) { content() } },
        )
    }
}
