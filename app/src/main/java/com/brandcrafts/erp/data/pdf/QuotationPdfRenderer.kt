package com.brandcrafts.erp.data.pdf

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.format.formatIndianCurrency
import com.brandcrafts.erp.domain.model.CompanyConfig
import com.brandcrafts.erp.domain.model.Contact
import com.brandcrafts.erp.domain.model.Quotation
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.Date
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class QuotationPdfRenderer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun render(
        quotation: Quotation,
        customer: Contact,
        company: CompanyConfig,
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            require(
                company.companyName.isNotBlank() && company.addressLine1.isNotBlank() &&
                    company.city.isNotBlank() && company.state.isNotBlank() &&
                    company.pincode.isNotBlank() && company.country.isNotBlank() &&
                    company.phone.isNotBlank() && company.email.isNotBlank(),
            )
            val directory = File(context.cacheDir, "pdf")
            check(directory.exists() || directory.mkdirs())
            val file = File(
                directory,
                "Quotation_${quotation.number.replace(Regex("[^A-Za-z0-9_-]"), "_")}.pdf",
            )

            val document = PdfDocument()
            try {
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }
                var pageNumber = 1
                var page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                var canvas = page.canvas
                var y = 48f

                fun text(value: String, bold: Boolean = false) {
                    paint.isFakeBoldText = bold
                    canvas.drawText(value, 40f, y, paint)
                    y += 18f
                }

                val columnRatios = floatArrayOf(.05f, .29f, .09f, .08f, .14f, .12f, .10f, .13f)
                val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.STROKE
                    strokeWidth = 0.75f
                }

                fun wrap(value: String, width: Float): List<String> {
                    val lines = mutableListOf<String>()
                    var remaining = value.trim()
                    while (remaining.isNotEmpty()) {
                        val count = paint.breakText(remaining, true, width, null)
                        if (count >= remaining.length) {
                            lines += remaining
                            break
                        }
                        val at = remaining.lastIndexOf(' ', count).takeIf { it > 0 } ?: count
                        lines += remaining.substring(0, at).trimEnd()
                        remaining = remaining.substring(at).trimStart()
                    }
                    return lines.ifEmpty { listOf("") }
                }

                fun drawTableRow(values: List<String>, height: Float, bold: Boolean = false) {
                    var x = BusinessPdfLayout.leftMargin
                    paint.isFakeBoldText = bold
                    values.forEachIndexed { index, value ->
                        val width = BusinessPdfLayout.contentWidth * columnRatios[index]
                        canvas.drawRect(x, y, x + width, y + height, borderPaint)
                        val lines = wrap(value, width - 10f)
                        var base = y + (height - lines.size * 14f) / 2f - paint.ascent()
                        lines.forEach { line ->
                            val textX = if (index == 1) x + 5f else x + width - 5f - paint.measureText(line)
                            canvas.drawText(line, textX, base, paint); base += 14f
                        }
                        x += width
                    }
                    y += height
                }

                fun tableHeader() {
                    drawTableRow(
                        listOf(
                            context.getString(R.string.quotation_pdf_column_serial),
                            context.getString(R.string.quotation_pdf_column_description),
                            context.getString(R.string.quotation_pdf_column_quantity),
                            context.getString(R.string.quotation_pdf_column_unit),
                            context.getString(R.string.quotation_pdf_column_rate),
                            context.getString(R.string.quotation_pdf_column_discount),
                            context.getString(R.string.quotation_pdf_column_tax),
                            context.getString(R.string.quotation_pdf_column_amount),
                        ),
                        24f,
                        true,
                    )
                }
                fun metadataRow(leftLabel: String, leftValue: String, rightLabel: String, rightValue: String) {
                    val top = y; val half = BusinessPdfLayout.contentWidth / 2f; val height = 30f
                    canvas.drawRect(BusinessPdfLayout.leftMargin, top, BusinessPdfLayout.leftMargin + BusinessPdfLayout.contentWidth, top + height, borderPaint)
                    canvas.drawLine(BusinessPdfLayout.leftMargin + half, top, BusinessPdfLayout.leftMargin + half, top + height, borderPaint)
                    paint.isFakeBoldText = true; canvas.drawText(leftLabel, BusinessPdfLayout.leftMargin + 5f, top + 12f, paint); canvas.drawText(rightLabel, BusinessPdfLayout.leftMargin + half + 5f, top + 12f, paint)
                    paint.isFakeBoldText = false; canvas.drawText(leftValue, BusinessPdfLayout.leftMargin + 5f, top + 25f, paint); canvas.drawText(rightValue, BusinessPdfLayout.leftMargin + half + 5f, top + 25f, paint); y += height
                }
                fun customerBlock() {
                    val rows = listOf(
                        context.getString(R.string.quotation_pdf_customer_name_label) to customer.company.ifBlank { customer.name },
                        context.getString(R.string.quotation_pdf_customer_address_label) to customer.address,
                        context.getString(R.string.quotation_pdf_customer_gst_label) to customer.gstNumber,
                        context.getString(R.string.quotation_pdf_customer_phone_label) to customer.phone,
                        context.getString(R.string.quotation_pdf_customer_email_label) to customer.email,
                    ).filter { it.second.isNotBlank() }
                    val height = 22f + rows.sumOf { wrap(it.second, 360f).size * 16 }.toFloat()
                    canvas.drawRect(BusinessPdfLayout.leftMargin, y, BusinessPdfLayout.leftMargin + BusinessPdfLayout.contentWidth, y + height, borderPaint)
                    paint.isFakeBoldText = true; canvas.drawText(context.getString(R.string.quotation_pdf_customer_details_title), BusinessPdfLayout.leftMargin + 5f, y + 15f, paint); y += 22f
                    paint.isFakeBoldText = false; rows.forEach { (label, value) -> wrap(value, 360f).forEachIndexed { index, line -> if (index == 0) canvas.drawText(label, BusinessPdfLayout.leftMargin + 5f, y + 12f, paint); canvas.drawText(line, BusinessPdfLayout.leftMargin + 130f, y + 12f, paint); y += 16f } }
                }

                fun drawFooter() {
                    val footerY = BusinessPdfLayout.pageHeight - BusinessPdfLayout.bottomMargin
                    canvas.drawLine(BusinessPdfLayout.leftMargin, footerY - 24f, BusinessPdfLayout.leftMargin + BusinessPdfLayout.contentWidth, footerY - 24f, borderPaint)
                    paint.isFakeBoldText = false
                    canvas.drawText(listOf(company.companyName, company.gstNumber, company.phone, company.email).filter(String::isNotBlank).joinToString(" | "), BusinessPdfLayout.leftMargin, footerY - 10f, paint)
                    canvas.drawText(context.getString(R.string.quotation_pdf_generated_label, java.text.DateFormat.getDateTimeInstance().format(java.util.Date())), BusinessPdfLayout.leftMargin, footerY + 4f, paint)
                    canvas.drawText(context.getString(R.string.quotation_pdf_page_label, pageNumber), BusinessPdfLayout.leftMargin + BusinessPdfLayout.contentWidth - 42f, footerY + 4f, paint)
                }

                fun newPage() {
                    drawFooter()
                    document.finishPage(page)
                    pageNumber += 1
                    page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                    canvas = page.canvas
                    y = 48f
                    val bannerHeight = BusinessPdfLayout.drawBanner(context, canvas, y)
                    if (bannerHeight > 0f) y += bannerHeight + 8f
                    text(context.getString(R.string.quotation_pdf_page_header, company.companyName, quotation.number), true)
                    tableHeader()
                }

                val bannerHeight = BusinessPdfLayout.drawBanner(context, canvas, y); if (bannerHeight > 0f) y += bannerHeight + 8f
                text(company.companyName, true); text(listOf(company.addressLine1, company.addressLine2, "${company.city}, ${company.state} ${company.pincode}").filter(String::isNotBlank).joinToString(" | ")); company.gstNumber.takeIf(String::isNotBlank)?.let { text(it) }; text("${company.phone}  ${company.email}")
                paint.isFakeBoldText = true; canvas.drawText(context.getString(R.string.quotation_pdf_document_title, quotation.number), BusinessPdfLayout.leftMargin + 190f, y, paint); y += 24f
                metadataRow(context.getString(R.string.quotation_pdf_quotation_number_label), quotation.number, context.getString(R.string.quotation_pdf_date_label), quotation.dateMillis?.let(::formatDate).orEmpty())
                metadataRow(
                    context.getString(R.string.quotation_pdf_status_label),
                    quotation.status.name,
                    context.getString(R.string.quotation_pdf_valid_until_label),
                    quotation.validUntilMillis?.let(::formatDate).orEmpty(),
                )
                customerBlock()
                tableHeader()

                quotation.lines.forEachIndexed { index, line ->
                    val rowHeight = maxOf(28f, wrap(line.description, BusinessPdfLayout.contentWidth * columnRatios[1] - 10f).size * 14f + 10f)
                    if (y + rowHeight > BusinessPdfLayout.footerBoundary) newPage()
                    drawTableRow(listOf((index + 1).toString(), line.description, line.quantity.toPlainString(), line.unit, formatIndianCurrency(line.unitPrice), line.discount.toPlainString(), line.tax.toPlainString(), formatIndianCurrency(line.total)), rowHeight)
                }
                fun section(title: String, value: String) {
                    val lines = wrap(value, BusinessPdfLayout.contentWidth - 10f); val height = 22f + lines.size * 16f + 8f
                    if (y + height > BusinessPdfLayout.footerBoundary) newPage()
                    canvas.drawRect(BusinessPdfLayout.leftMargin, y, BusinessPdfLayout.leftMargin + BusinessPdfLayout.contentWidth, y + height, borderPaint)
                    paint.isFakeBoldText = true; canvas.drawText(title, BusinessPdfLayout.leftMargin + 5f, y + 15f, paint); paint.isFakeBoldText = false; y += 22f
                    lines.forEach { line -> canvas.drawText(line, BusinessPdfLayout.leftMargin + 5f, y + 12f, paint); y += 16f }; y += 8f
                }
                val summaryHeight = 30f
                if (y + summaryHeight > BusinessPdfLayout.footerBoundary) newPage()
                val summaryLeft = BusinessPdfLayout.leftMargin + BusinessPdfLayout.contentWidth * .55f
                canvas.drawRect(summaryLeft, y, BusinessPdfLayout.leftMargin + BusinessPdfLayout.contentWidth, y + summaryHeight, borderPaint)
                paint.isFakeBoldText = true; canvas.drawText(context.getString(R.string.quotation_pdf_grand_total_plain), summaryLeft + 5f, y + 19f, paint)
                val amount = formatIndianCurrency(quotation.grandTotal); canvas.drawText(amount, BusinessPdfLayout.leftMargin + BusinessPdfLayout.contentWidth - 5f - paint.measureText(amount), y + 19f, paint); y += summaryHeight
                if (quotation.remarks.isNotBlank()) section(context.getString(R.string.quotation_pdf_notes_title), quotation.remarks)
                if (company.quotationTerms.isNotBlank()) section(context.getString(R.string.quotation_pdf_terms_title), company.quotationTerms)
                val signatureHeight = 72f
                if (y + signatureHeight > BusinessPdfLayout.footerBoundary) newPage()
                y += 18f
                paint.isFakeBoldText = true
                val companyLine = context.getString(R.string.quotation_pdf_for_company, company.companyName)
                canvas.drawText(companyLine, BusinessPdfLayout.leftMargin + BusinessPdfLayout.contentWidth - 5f - paint.measureText(companyLine), y, paint)
                y += 42f
                canvas.drawText(context.getString(R.string.quotation_pdf_authorized_signature), BusinessPdfLayout.leftMargin + BusinessPdfLayout.contentWidth - 125f, y, paint)
                drawFooter()
                document.finishPage(page)
                file.outputStream().use(document::writeTo)
            } finally {
                document.close()
            }
            Result.success(file)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Throwable) {
            Result.failure(exception)
        }
    }

    private fun formatDate(millis: Long): String =
        java.text.DateFormat.getDateInstance().format(Date(millis))
}
