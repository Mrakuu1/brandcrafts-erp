package com.brandcrafts.erp.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.font.FontWeight
import com.brandcrafts.erp.ui.theme.BrandCraftsTheme

enum class StatusTone { NEUTRAL, SUCCESS, WARNING, ERROR, INFO }

@Composable
fun StatusChip(
    label: String,
    modifier: Modifier = Modifier,
    tone: StatusTone = StatusTone.NEUTRAL,
) {
    val dark = MaterialTheme.colorScheme.background.red < .2f
    val (dotColor, lightBackground) = when (tone) {
        StatusTone.NEUTRAL -> Color(0xFF667085) to Color(0xFFF1F3F5)
        StatusTone.SUCCESS -> Color(0xFF159447) to Color(0xFFE5F6EB)
        StatusTone.WARNING -> Color(0xFFFFA000) to Color(0xFFFFF4D7)
        StatusTone.ERROR -> Color(0xFFE53935) to Color(0xFFFFE8E7)
        StatusTone.INFO -> Color(0xFF2769B4) to Color(0xFFE8F1FF)
    }
    Row(
        modifier = modifier
            .semantics { contentDescription = label }
            .clip(RoundedCornerShape(10.dp))
            .background(if (dark) dotColor.copy(alpha = .17f) else lightBackground)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(dotColor, CircleShape),
        )
        Text(
            text = label,
            modifier = Modifier,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = dotColor,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun StatusChipPreview() {
    BrandCraftsTheme { StatusChip(label = "Pending", tone = StatusTone.WARNING) }
}
