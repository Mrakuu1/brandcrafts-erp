package com.brandcrafts.erp.data.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.RectF
import com.brandcrafts.erp.R
import kotlin.math.roundToInt

/** Shared A4 print metrics and safe banner handling for business-document PDFs. */
object BusinessPdfLayout {
    const val pageWidth = 595
    const val pageHeight = 842
    const val leftMargin = 42f
    const val rightMargin = 42f
    const val topMargin = 36f
    const val bottomMargin = 42f
    const val contentWidth = pageWidth - leftMargin - rightMargin
    const val footerHeight = 16f
    const val footerBoundary = pageHeight - bottomMargin - footerHeight
    const val logoWidth = 135f

    /** Draws the 2172 x 724 branding image as a compact, left-aligned letterhead logo. */
    fun drawBanner(context: Context, canvas: Canvas, top: Float): Float {
        val bitmap = loadBanner(context) ?: return 0f
        return try {
            val height = logoWidth * bitmap.height.toFloat() / bitmap.width.toFloat()
            canvas.drawBitmap(bitmap, null, RectF(leftMargin, top, leftMargin + logoWidth, top + height), null)
            height
        } finally {
            bitmap.recycle()
        }
    }

    private fun loadBanner(context: Context): Bitmap? = try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeResource(context.resources, R.drawable.banner_image, bounds)
        var sample = 1
        while (bounds.outWidth / sample > contentWidth.roundToInt() * 2) sample *= 2
        BitmapFactory.decodeResource(
            context.resources,
            R.drawable.banner_image,
            BitmapFactory.Options().apply { inSampleSize = sample },
        )
    } catch (_: Throwable) {
        null
    }
}
