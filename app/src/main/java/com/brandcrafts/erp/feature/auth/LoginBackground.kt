package com.brandcrafts.erp.feature.auth

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

/** Decorative, resolution-independent background used only by the authentication flow. */
@Composable
fun LoginBackground(modifier: Modifier = Modifier, darkMode: Boolean = MaterialTheme.colorScheme.background.luminance() < .5f) {
    val background = if (darkMode) Color(0xFF070D14) else Color(0xFFFFFCFA)
    val waveFill = if (darkMode) Color(0xFF2A160D).copy(alpha = .30f) else Color(0xFFFFE3D1).copy(alpha = .38f)
    val waveLine = if (darkMode) Color(0xFFFF6A00).copy(alpha = .26f) else Color(0xFFFF7A00).copy(alpha = .18f)
    val dots = if (darkMode) Color(0xFFFF6A00).copy(alpha = .24f) else Color(0xFFFF7A00).copy(alpha = .14f)

    Canvas(modifier = modifier) {
        drawRect(background)
        drawMiddleWave(waveFill, waveLine)
        drawMiddleWave2(waveFill, waveLine)
        drawBottomWave(waveFill, waveLine)
        drawDotMatrix(
            origin = Offset(size.width * .77f, size.height * .06f),
            color = dots,
            columns = 7,
            rows = 9,
        )
        drawDotMatrix(
            origin = Offset(size.width * .06f, size.height * .84f),
            color = dots,
            columns = 9,
            rows = 6,
        )
    }
}

private fun DrawScope.drawMiddleWave(fill: Color, line: Color) {
    // Keep the decorative band below the sign-in form; it must never compete
    // with the Welcome back heading or the text-entry area.
    val top = size.height * .84f
    val wave = Path().apply {
        moveTo(0f, top)
        cubicTo(size.width * .18f, top + 8f, size.width * .36f, top + 60f, size.width * .54f, top + 56f)
        cubicTo(size.width * .74f, top + 48f, size.width * .88f, top + 92f, size.width, top + 106f)
        lineTo(size.width, top + 132f)
        cubicTo(size.width * .77f, top + 106f, size.width * .56f, top + 84f, size.width * .34f, top + 94f)
        cubicTo(size.width * .16f, top + 102f, size.width * .07f, top + 42f, 0f, top + 30f)
        close()
    }
    drawPath(wave, fill)
    drawPath(wave, line, style = Stroke(width = 1.2f))
}

private fun DrawScope.drawMiddleWave2(fill: Color, line: Color) {
    // Keep the decorative band below the sign-in form; it must never compete
    // with the Welcome back heading or the text-entry area.
    val top = size.height * .80f
    val wave = Path().apply {
        moveTo(0f, top)
        cubicTo(size.width * .18f, top + 8f, size.width * .36f, top + 60f, size.width * .54f, top + 56f)
        cubicTo(size.width * .74f, top + 48f, size.width * .88f, top + 92f, size.width, top + 106f)
        lineTo(size.width, top + 132f)
        cubicTo(size.width * .77f, top + 106f, size.width * .56f, top + 84f, size.width * .34f, top + 94f)
        cubicTo(size.width * .16f, top + 102f, size.width * .07f, top + 42f, 0f, top + 30f)
        close()
    }
    drawPath(wave, fill)
    drawPath(wave, line, style = Stroke(width = 1.2f))
}

private fun DrawScope.drawBottomWave(fill: Color, line: Color) {
    val top = size.height * .84f
    val wave = Path().apply {
        moveTo(0f, top)
        cubicTo(size.width * .20f, top - 44f, size.width * .34f, top + 8f, size.width * .58f, top - 20f)
        cubicTo(size.width * .76f, top - 42f, size.width * .88f, top + 34f, size.width, top + 18f)
        lineTo(size.width, size.height)
        lineTo(0f, size.height)
        close()
    }
    drawPath(wave, fill)
    drawPath(wave, line, style = Stroke(width = 1.2f))
}

private fun DrawScope.drawDotMatrix(
    origin: Offset,
    color: Color,
    columns: Int,
    rows: Int,
) {
    val gap = 11f
    repeat(columns) { column ->
        repeat(rows) { row ->
            drawCircle(
                color = color.copy(alpha = color.alpha * ((column + row + 3).toFloat() / (columns + rows + 2))),
                radius = 1.8f,
                center = Offset(origin.x + column * gap, origin.y + row * gap),
            )
        }
    }
}
