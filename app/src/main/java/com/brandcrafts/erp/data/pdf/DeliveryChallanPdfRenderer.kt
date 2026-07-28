package com.brandcrafts.erp.data.pdf

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.brandcrafts.erp.R
import com.brandcrafts.erp.domain.model.CompanyConfig
import com.brandcrafts.erp.domain.model.Contact
import com.brandcrafts.erp.domain.model.DeliveryChallan
import com.brandcrafts.erp.domain.model.DeliveryChallanLine
import com.brandcrafts.erp.domain.model.DeliveryChallanStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Produces a print-safe, non-financial A4 Delivery Challan in the app cache. */
class DeliveryChallanPdfRenderer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun render(
        challan: DeliveryChallan,
        customer: Contact,
        company: CompanyConfig,
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            company.requirePdfIdentity()
            val directory = File(context.cacheDir, PDF_DIRECTORY)
            check(directory.exists() || directory.mkdirs())
            val output = File(directory, deliveryChallanFileName(challan.number))
            val document = PdfDocument()
            try {
                DeliveryChallanPdfLayout(document, challan, customer, company).render()
                output.outputStream().use(document::writeTo)
            } finally {
                document.close()
            }
            Result.success(output)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Throwable) {
            Result.failure(exception)
        }
    }

    private inner class DeliveryChallanPdfLayout(
        private val document: PdfDocument,
        private val challan: DeliveryChallan,
        private val customer: Contact,
        private val company: CompanyConfig,
    ) {
        private val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = BODY_TEXT_SIZE }
        private val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = SECTION_TEXT_SIZE
            isFakeBoldText = true
        }
        private val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            textSize = TITLE_TEXT_SIZE
            isFakeBoldText = true
        }
        private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = BORDER_THICKNESS
        }
        private val generatedAt = formatDateTime(System.currentTimeMillis())
        private var pageNumber = 0
        private lateinit var page: PdfDocument.Page
        private lateinit var canvas: Canvas
        private var currentY = TOP_MARGIN

        fun render() {
            startFirstPage()
            challan.lines.sortedBy(DeliveryChallanLine::sortOrder).forEachIndexed { index, line ->
                drawItemRow(index + 1, line)
            }
            drawNotes()
            drawSignatureBlock()
            finishPage()
        }

        private fun startFirstPage() {
            startPage()
            drawBannerOrCompanyHeader()
            drawCentered(context.getString(R.string.delivery_challan_pdf_document_title, challan.number), titlePaint)
            advance(SECTION_SPACING)
            drawInfoTable()
            advance(SECTION_SPACING)
            drawCustomerDeliverySection()
            advance(SECTION_SPACING)
            drawItemsHeader()
        }

        private fun startItemContinuationPage() {
            startContentContinuationPage()
            drawItemsHeader()
        }

        private fun startContentContinuationPage() {
            finishPage()
            startPage()
            drawCompactContinuationHeader()
        }

        private fun startPage() {
            pageNumber += 1
            page = document.startPage(pageInfo(pageNumber))
            canvas = page.canvas
            currentY = TOP_MARGIN
        }

        private fun finishPage() {
            drawFooter()
            document.finishPage(page)
        }

        private fun drawBannerOrCompanyHeader() {
            val bannerHeight = BusinessPdfLayout.drawBanner(context, canvas, currentY)
            if (bannerHeight > 0f) {
                currentY += bannerHeight + NORMAL_SPACING
            } else {
                drawCentered(company.companyName, sectionPaint)
                company.legalName.takeIf(String::isNotBlank)?.let { drawCentered(it, bodyPaint) }
                drawCentered(companyAddress(), bodyPaint)
                drawCentered(context.getString(R.string.delivery_challan_pdf_company_contact, company.phone, company.email), bodyPaint)
                currentY += NORMAL_SPACING
            }
        }

        private fun drawCompactContinuationHeader() {
            drawText(context.getString(R.string.delivery_challan_pdf_document_title, challan.number), LEFT_MARGIN, currentY, sectionPaint)
            drawRightText(
                context.getString(R.string.delivery_challan_pdf_date_label, formatDate(challan.dateMillis)),
                LEFT_MARGIN + CONTENT_WIDTH,
                currentY,
                bodyPaint,
            )
            advance(LINE_HEIGHT + NORMAL_SPACING)
        }

        private fun drawInfoTable() {
            val leftWidth = CONTENT_WIDTH * INFO_LEFT_COLUMN_RATIO
            val rows = listOf(
                InfoRow(
                    context.getString(R.string.delivery_challan_pdf_info_number),
                    challan.number,
                    context.getString(R.string.delivery_challan_pdf_info_date),
                    formatDate(challan.dateMillis),
                ),
                InfoRow(
                    context.getString(R.string.delivery_challan_pdf_info_status),
                    context.getString(challan.status.labelRes()),
                    context.getString(R.string.delivery_challan_pdf_info_source_invoice),
                    challan.sourceInvoiceNumber.orEmpty(),
                ),
            )
            rows.forEach { row ->
                val rowHeight = max(
                    cellHeight(row.leftLabel, leftWidth * LABEL_RATIO, sectionPaint) + cellHeight(row.leftValue, leftWidth * VALUE_RATIO, bodyPaint),
                    cellHeight(row.rightLabel, (CONTENT_WIDTH - leftWidth) * LABEL_RATIO, sectionPaint) + cellHeight(row.rightValue, (CONTENT_WIDTH - leftWidth) * VALUE_RATIO, bodyPaint),
                ).coerceAtLeast(INFO_ROW_HEIGHT)
                ensureSpace(rowHeight)
                drawRect(LEFT_MARGIN, currentY, CONTENT_WIDTH, rowHeight)
                drawVerticalLine(LEFT_MARGIN + leftWidth, currentY, currentY + rowHeight)
                drawLabelValueCell(LEFT_MARGIN, currentY, leftWidth, rowHeight, row.leftLabel, row.leftValue)
                drawLabelValueCell(LEFT_MARGIN + leftWidth, currentY, CONTENT_WIDTH - leftWidth, rowHeight, row.rightLabel, row.rightValue)
                currentY += rowHeight
            }
        }

        private fun drawCustomerDeliverySection() {
            val rows = mutableListOf<Pair<String, String>>().apply {
                add(context.getString(R.string.delivery_challan_pdf_customer_label) to customer.company.ifBlank { customer.name })
                add(context.getString(R.string.delivery_challan_pdf_delivery_address_label) to challan.deliveryAddress)
                challan.vehicleNumber.takeIf(String::isNotBlank)?.let { add(context.getString(R.string.delivery_challan_pdf_vehicle_label) to it) }
                challan.driverName.takeIf(String::isNotBlank)?.let { add(context.getString(R.string.delivery_challan_pdf_driver_label) to it) }
            }
            val height = SECTION_HEADER_HEIGHT + rows.sumOf { (_, value) ->
                cellHeight(value, CONTENT_WIDTH - CUSTOMER_LABEL_WIDTH - CELL_PADDING * 3, bodyPaint).roundToInt()
            } + rows.size * ROW_PADDING.roundToInt()
            ensureSpace(height.toFloat())
            val sectionTop = currentY
            drawRect(LEFT_MARGIN, sectionTop, CONTENT_WIDTH, height.toFloat())
            drawFilledSectionHeader(context.getString(R.string.delivery_challan_pdf_customer_delivery_details), sectionTop)
            currentY += SECTION_HEADER_HEIGHT
            rows.forEach { (label, value) ->
                val rowHeight = cellHeight(value, CONTENT_WIDTH - CUSTOMER_LABEL_WIDTH - CELL_PADDING * 3, bodyPaint) + ROW_PADDING
                drawText(label, LEFT_MARGIN + CELL_PADDING, currentY + CELL_PADDING - sectionPaint.ascent(), sectionPaint)
                drawWrappedText(
                    value,
                    LEFT_MARGIN + CUSTOMER_LABEL_WIDTH + CELL_PADDING,
                    currentY + CELL_PADDING - bodyPaint.ascent(),
                    CONTENT_WIDTH - CUSTOMER_LABEL_WIDTH - CELL_PADDING * 3,
                    bodyPaint,
                )
                currentY += rowHeight
            }
            currentY = sectionTop + height
        }

        private fun drawItemsHeader() {
            ensureSpace(TABLE_HEADER_HEIGHT)
            drawTableRow(
                top = currentY,
                height = TABLE_HEADER_HEIGHT,
                values = listOf(
                    context.getString(R.string.delivery_challan_pdf_column_serial),
                    context.getString(R.string.delivery_challan_pdf_column_description),
                    context.getString(R.string.delivery_challan_pdf_column_quantity),
                    context.getString(R.string.delivery_challan_pdf_column_unit),
                ),
                paint = sectionPaint,
                alignments = listOf(Alignment.CENTER, Alignment.LEFT, Alignment.RIGHT, Alignment.CENTER),
            )
            currentY += TABLE_HEADER_HEIGHT
        }

        private fun drawItemRow(serial: Int, line: DeliveryChallanLine) {
            val widths = itemColumnWidths()
            val values = listOf(serial.toString(), line.description, line.quantity.toPlainString(), line.unit)
            val rowHeight = values.mapIndexed { index, value ->
                cellHeight(value, widths[index] - CELL_PADDING * 2, bodyPaint)
            }.maxOrNull()?.plus(ROW_PADDING * 2) ?: MIN_ITEM_ROW_HEIGHT
            if (currentY + rowHeight > contentBottom()) startItemContinuationPage()
            drawTableRow(
                top = currentY,
                height = rowHeight,
                values = values,
                paint = bodyPaint,
                alignments = listOf(Alignment.CENTER, Alignment.LEFT, Alignment.RIGHT, Alignment.CENTER),
            )
            currentY += rowHeight
        }

        private fun drawNotes() {
            val notes = challan.notes.takeIf(String::isNotBlank) ?: return
            val wrapped = wrapText(notes, bodyPaint, CONTENT_WIDTH - CELL_PADDING * 2)
            val height = SECTION_HEADER_HEIGHT + wrapped.size * LINE_HEIGHT + ROW_PADDING * 2
            if (currentY + SECTION_SPACING + height > contentBottom()) startContentContinuationPage()
            advance(SECTION_SPACING)
            drawRect(LEFT_MARGIN, currentY, CONTENT_WIDTH, height)
            drawFilledSectionHeader(context.getString(R.string.delivery_challan_pdf_notes_title), currentY)
            currentY += SECTION_HEADER_HEIGHT + ROW_PADDING
            wrapped.forEach { line ->
                drawText(line, LEFT_MARGIN + CELL_PADDING, currentY - bodyPaint.ascent(), bodyPaint)
                currentY += LINE_HEIGHT
            }
            currentY += ROW_PADDING
        }

        private fun drawSignatureBlock() {
            if (currentY + SECTION_SPACING + SIGNATURE_HEIGHT > contentBottom()) startContentContinuationPage()
            advance(SECTION_SPACING)
            val top = currentY
            drawRect(LEFT_MARGIN, top, CONTENT_WIDTH, SIGNATURE_HEIGHT)
            val divider = LEFT_MARGIN + CONTENT_WIDTH / 2f
            drawVerticalLine(divider, top, top + SIGNATURE_HEIGHT)
            drawText(context.getString(R.string.delivery_challan_pdf_received_by_title), LEFT_MARGIN + CELL_PADDING, top + SIGNATURE_LABEL_OFFSET, sectionPaint)
            drawText(context.getString(R.string.delivery_challan_pdf_received_name_blank), LEFT_MARGIN + CELL_PADDING, top + SIGNATURE_NAME_OFFSET, bodyPaint)
            drawText(context.getString(R.string.delivery_challan_pdf_received_signature_blank), LEFT_MARGIN + CELL_PADDING, top + SIGNATURE_SIGNATURE_OFFSET, bodyPaint)
            drawText(context.getString(R.string.delivery_challan_pdf_received_date_blank), LEFT_MARGIN + CELL_PADDING, top + SIGNATURE_DATE_OFFSET, bodyPaint)
            drawText(context.getString(R.string.delivery_challan_pdf_for_company, company.companyName), divider + CELL_PADDING, top + SIGNATURE_LABEL_OFFSET, sectionPaint)
            drawText(context.getString(R.string.delivery_challan_pdf_authorized_signature), divider + CELL_PADDING, top + SIGNATURE_SIGNATURE_OFFSET, bodyPaint)
            currentY += SIGNATURE_HEIGHT
        }

        private fun drawTableRow(
            top: Float,
            height: Float,
            values: List<String>,
            paint: Paint,
            alignments: List<Alignment>,
        ) {
            val widths = itemColumnWidths()
            var x = LEFT_MARGIN
            values.forEachIndexed { index, value ->
                val width = widths[index]
                drawRect(x, top, width, height)
                drawCellText(value, x, top, width, height, paint, alignments[index])
                x += width
            }
        }

        private fun drawCellText(
            value: String,
            x: Float,
            top: Float,
            width: Float,
            height: Float,
            paint: Paint,
            alignment: Alignment,
        ) {
            val lines = wrapText(value, paint, width - CELL_PADDING * 2)
            val totalTextHeight = lines.size * LINE_HEIGHT
            var baseline = top + (height - totalTextHeight) / 2f - paint.ascent()
            lines.forEach { line ->
                val textX = when (alignment) {
                    Alignment.LEFT -> x + CELL_PADDING
                    Alignment.CENTER -> x + (width - paint.measureText(line)) / 2f
                    Alignment.RIGHT -> x + width - CELL_PADDING - paint.measureText(line)
                }
                drawText(line, textX, baseline, paint)
                baseline += LINE_HEIGHT
            }
        }

        private fun drawFilledSectionHeader(title: String, top: Float) {
            drawText(title, LEFT_MARGIN + CELL_PADDING, top + SECTION_HEADER_TEXT_OFFSET, sectionPaint)
            canvas.drawLine(LEFT_MARGIN, top + SECTION_HEADER_HEIGHT, LEFT_MARGIN + CONTENT_WIDTH, top + SECTION_HEADER_HEIGHT, borderPaint)
        }

        private fun drawFooter() {
            val baseline = PAGE_HEIGHT - BOTTOM_MARGIN / 2f
            drawText(context.getString(R.string.delivery_challan_pdf_generated_label, generatedAt), LEFT_MARGIN, baseline, bodyPaint)
            drawRightText(
                context.getString(R.string.delivery_challan_pdf_page_label, pageNumber),
                LEFT_MARGIN + CONTENT_WIDTH,
                baseline,
                bodyPaint,
            )
        }

        private fun ensureSpace(requiredHeight: Float) {
            if (currentY + requiredHeight > contentBottom()) startItemContinuationPage()
        }

        private fun contentBottom(): Float = PAGE_HEIGHT - BOTTOM_MARGIN - FOOTER_HEIGHT

        private fun advance(amount: Float) {
            currentY += amount
        }

        private fun drawRect(x: Float, y: Float, width: Float, height: Float) {
            canvas.drawRect(x, y, x + width, y + height, borderPaint)
        }

        private fun drawVerticalLine(x: Float, top: Float, bottom: Float) {
            canvas.drawLine(x, top, x, bottom, borderPaint)
        }

        private fun drawText(value: String, x: Float, baseline: Float, paint: Paint) {
            paint.isFakeBoldText = paint === sectionPaint || paint === titlePaint
            canvas.drawText(value, x, baseline, paint)
        }

        private fun drawCentered(value: String, paint: Paint) {
            drawText(value, LEFT_MARGIN + (CONTENT_WIDTH - paint.measureText(value)) / 2f, currentY - paint.ascent(), paint)
            advance(LINE_HEIGHT)
        }

        private fun drawRightText(value: String, right: Float, baseline: Float, paint: Paint) {
            drawText(value, right - paint.measureText(value), baseline, paint)
        }

        private fun drawLabelValueCell(x: Float, top: Float, width: Float, height: Float, label: String, value: String) {
            val labelWidth = width * LABEL_RATIO
            drawText(label, x + CELL_PADDING, top + CELL_PADDING - sectionPaint.ascent(), sectionPaint)
            drawWrappedText(
                value.ifBlank { context.getString(R.string.delivery_challan_pdf_value_unavailable) },
                x + labelWidth + CELL_PADDING,
                top + CELL_PADDING - bodyPaint.ascent(),
                width - labelWidth - CELL_PADDING * 2,
                bodyPaint,
            )
        }

        private fun drawWrappedText(value: String, x: Float, baseline: Float, width: Float, paint: Paint) {
            var y = baseline
            wrapText(value, paint, width).forEach { line ->
                drawText(line, x, y, paint)
                y += LINE_HEIGHT
            }
        }

        private fun cellHeight(value: String, width: Float, paint: Paint): Float =
            wrapText(value.ifBlank { context.getString(R.string.delivery_challan_pdf_value_unavailable) }, paint, width).size * LINE_HEIGHT + CELL_PADDING * 2

        private fun wrapText(value: String, paint: Paint, width: Float): List<String> {
            if (value.isBlank()) return listOf("")
            val lines = mutableListOf<String>()
            var remaining = value.trim()
            while (remaining.isNotEmpty()) {
                val count = paint.breakText(remaining, true, width, null)
                if (count >= remaining.length) {
                    lines += remaining
                    break
                }
                val breakAt = remaining.lastIndexOf(' ', count).takeIf { it > 0 } ?: count
                lines += remaining.substring(0, breakAt).trimEnd()
                remaining = remaining.substring(breakAt).trimStart()
            }
            return lines
        }

        private fun itemColumnWidths(): List<Float> = listOf(
            CONTENT_WIDTH * SERIAL_COLUMN_RATIO,
            CONTENT_WIDTH * DESCRIPTION_COLUMN_RATIO,
            CONTENT_WIDTH * QUANTITY_COLUMN_RATIO,
            CONTENT_WIDTH * UNIT_COLUMN_RATIO,
        )

        private fun companyAddress(): String = listOf(
            company.addressLine1,
            company.addressLine2,
            "${company.city}, ${company.state} ${company.pincode}",
            company.country,
        ).filter(String::isNotBlank).joinToString(" | ")
    }

    private fun CompanyConfig.requirePdfIdentity() {
        require(
            companyName.isNotBlank() &&
                addressLine1.isNotBlank() &&
                city.isNotBlank() &&
                state.isNotBlank() &&
                pincode.isNotBlank() &&
                country.isNotBlank() &&
                phone.isNotBlank() &&
                email.isNotBlank(),
        )
    }

    private fun deliveryChallanFileName(number: String): String =
        "DeliveryChallan_${number.replace(Regex("[^A-Za-z0-9_-]"), "_")}.pdf"

    private fun formatDate(value: Long): String =
        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale("en", "IN")).format(Date(value))

    private fun formatDateTime(value: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale("en", "IN")).format(Date(value))

    private fun DeliveryChallanStatus.labelRes(): Int = when (this) {
        DeliveryChallanStatus.DRAFT -> R.string.delivery_challan_status_draft
        DeliveryChallanStatus.DISPATCHED -> R.string.delivery_challan_status_dispatched
        DeliveryChallanStatus.CANCELLED -> R.string.delivery_challan_status_cancelled
    }

    private fun pageInfo(pageNumber: Int): PdfDocument.PageInfo =
        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()

    private data class InfoRow(
        val leftLabel: String,
        val leftValue: String,
        val rightLabel: String,
        val rightValue: String,
    )

    private enum class Alignment { LEFT, CENTER, RIGHT }

    private companion object {
        const val PDF_DIRECTORY = "pdf"
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val LEFT_MARGIN = 42f
        const val RIGHT_MARGIN = 42f
        const val TOP_MARGIN = 36f
        const val BOTTOM_MARGIN = 42f
        const val CONTENT_WIDTH = PAGE_WIDTH - LEFT_MARGIN - RIGHT_MARGIN
        const val NORMAL_SPACING = 8f
        const val SECTION_SPACING = 14f
        const val ROW_PADDING = 5f
        const val CELL_PADDING = 5f
        const val BORDER_THICKNESS = 0.75f
        const val BODY_TEXT_SIZE = 10f
        const val SECTION_TEXT_SIZE = 10.5f
        const val TITLE_TEXT_SIZE = 17f
        const val LINE_HEIGHT = 14f
        const val FOOTER_HEIGHT = 16f
        const val SECTION_HEADER_HEIGHT = 22f
        const val SECTION_HEADER_TEXT_OFFSET = 15f
        const val INFO_ROW_HEIGHT = 30f
        const val TABLE_HEADER_HEIGHT = 24f
        const val MIN_ITEM_ROW_HEIGHT = 28f
        const val SIGNATURE_HEIGHT = 106f
        const val SIGNATURE_LABEL_OFFSET = 20f
        const val SIGNATURE_NAME_OFFSET = 48f
        const val SIGNATURE_SIGNATURE_OFFSET = 70f
        const val SIGNATURE_DATE_OFFSET = 92f
        const val CUSTOMER_LABEL_WIDTH = 110f
        const val LABEL_RATIO = 0.38f
        const val VALUE_RATIO = 0.62f
        const val INFO_LEFT_COLUMN_RATIO = 0.5f
        const val SERIAL_COLUMN_RATIO = 0.09f
        const val DESCRIPTION_COLUMN_RATIO = 0.57f
        const val QUANTITY_COLUMN_RATIO = 0.18f
        const val UNIT_COLUMN_RATIO = 0.16f
    }
}
