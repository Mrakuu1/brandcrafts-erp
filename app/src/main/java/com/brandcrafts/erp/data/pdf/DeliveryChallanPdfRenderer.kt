package com.brandcrafts.erp.data.pdf

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.brandcrafts.erp.R
import com.brandcrafts.erp.domain.model.CompanyConfig
import com.brandcrafts.erp.domain.model.Contact
import com.brandcrafts.erp.domain.model.DeliveryChallan
import com.brandcrafts.erp.domain.model.DeliveryChallanStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Produces a cache-only, non-financial Delivery Challan PDF from domain data. */
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
                renderDocument(document, challan, customer, company)
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

    private fun renderDocument(
        document: PdfDocument,
        challan: DeliveryChallan,
        customer: Contact,
        company: CompanyConfig,
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = BODY_TEXT_SIZE }
        var pageNumber = 1
        var page = document.startPage(pageInfo(pageNumber))
        var canvas = page.canvas
        var y = TOP_MARGIN
        lateinit var newPage: () -> Unit

        fun drawLine(value: String, bold: Boolean = false) {
            paint.isFakeBoldText = bold
            canvas.drawText(value, HORIZONTAL_MARGIN, y, paint)
            y += LINE_HEIGHT
        }

        fun drawWrapped(value: String, bold: Boolean = false) {
            paint.isFakeBoldText = bold
            value.wrapToWidth(paint, CONTENT_WIDTH).forEach { line ->
                if (y > CONTENT_BOTTOM) newPage()
                canvas.drawText(line, HORIZONTAL_MARGIN, y, paint)
                y += LINE_HEIGHT
            }
        }

        fun drawFooter() {
            paint.isFakeBoldText = false
            canvas.drawText(
                context.getString(R.string.delivery_challan_pdf_page_label, pageNumber),
                HORIZONTAL_MARGIN,
                PAGE_HEIGHT - FOOTER_MARGIN,
                paint,
            )
        }

        fun drawTableHeader() {
            drawLine(context.getString(R.string.delivery_challan_pdf_table_header), bold = true)
        }

        fun drawHeader() {
            drawWrapped(company.companyName, bold = true)
            company.legalName.takeIf(String::isNotBlank)?.let { drawWrapped(it) }
            drawWrapped(
                context.getString(
                    R.string.delivery_challan_pdf_company_address,
                    company.addressLine1,
                    company.addressLine2,
                    company.city,
                    company.state,
                    company.pincode,
                    company.country,
                ),
            )
            drawWrapped(context.getString(R.string.delivery_challan_pdf_company_contact, company.phone, company.email))
            company.gstNumber.takeIf(String::isNotBlank)?.let {
                drawWrapped(context.getString(R.string.delivery_challan_pdf_gst_label, it))
            }
            drawWrapped(context.getString(R.string.delivery_challan_pdf_document_title, challan.number), bold = true)
            drawWrapped(
                context.getString(
                    R.string.delivery_challan_pdf_status_label,
                    context.getString(challan.status.labelRes()),
                ),
            )
            drawWrapped(
                context.getString(
                    R.string.delivery_challan_pdf_customer_label,
                    customer.company.ifBlank { customer.name },
                ),
            )
            drawWrapped(context.getString(R.string.delivery_challan_pdf_delivery_address_label, challan.deliveryAddress))
            customer.phone.takeIf(String::isNotBlank)?.let {
                drawWrapped(context.getString(R.string.delivery_challan_pdf_phone_label, it))
            }
            customer.email.takeIf(String::isNotBlank)?.let {
                drawWrapped(context.getString(R.string.delivery_challan_pdf_email_label, it))
            }
            drawWrapped(context.getString(R.string.delivery_challan_pdf_date_label, formatDate(challan.dateMillis)))
            challan.sourceInvoiceNumber?.takeIf(String::isNotBlank)?.let {
                drawWrapped(context.getString(R.string.delivery_challan_pdf_source_invoice_label, it))
            }
            challan.vehicleNumber.takeIf(String::isNotBlank)?.let {
                drawWrapped(context.getString(R.string.delivery_challan_pdf_vehicle_label, it))
            }
            challan.driverName.takeIf(String::isNotBlank)?.let {
                drawWrapped(context.getString(R.string.delivery_challan_pdf_driver_label, it))
            }
            if (challan.status == DeliveryChallanStatus.DISPATCHED) {
                drawWrapped(context.getString(R.string.delivery_challan_pdf_dispatched_by_label))
            }
            drawTableHeader()
        }

        newPage = {
            drawFooter()
            document.finishPage(page)
            pageNumber += 1
            page = document.startPage(pageInfo(pageNumber))
            canvas = page.canvas
            y = TOP_MARGIN
            drawHeader()
        }

        drawHeader()
        challan.lines.sortedBy { it.sortOrder }.forEachIndexed { index, line ->
            val itemLines = context.getString(
                R.string.delivery_challan_pdf_table_row,
                index + 1,
                line.description,
                line.quantity.toPlainString(),
                line.unit,
            ).wrapToWidth(paint, CONTENT_WIDTH)
            if (y + itemLines.size * LINE_HEIGHT > CONTENT_BOTTOM) newPage()
            paint.isFakeBoldText = false
            itemLines.forEach { value ->
                canvas.drawText(value, HORIZONTAL_MARGIN, y, paint)
                y += LINE_HEIGHT
            }
        }
        challan.notes.takeIf(String::isNotBlank)?.let {
            if (y > CONTENT_BOTTOM - LINE_HEIGHT * 3) newPage()
            drawWrapped(context.getString(R.string.delivery_challan_pdf_notes_label, it))
        }
        if (y > CONTENT_BOTTOM - LINE_HEIGHT * 6) newPage()
        drawWrapped(context.getString(R.string.delivery_challan_pdf_received_by_label))
        drawWrapped(context.getString(R.string.delivery_challan_pdf_received_signature_label))
        drawWrapped(
            context.getString(
                R.string.delivery_challan_pdf_authorized_signature_label,
                company.authorizedSignatoryName,
                company.authorizedSignatoryDesignation,
            ),
        )
        drawWrapped(context.getString(R.string.delivery_challan_pdf_generated_label, formatDateTime(System.currentTimeMillis())))
        drawFooter()
        document.finishPage(page)
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

    private fun String.wrapToWidth(paint: Paint, width: Float): List<String> {
        if (isBlank()) return listOf("")
        val lines = mutableListOf<String>()
        var remaining = trim()
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

    private fun deliveryChallanFileName(number: String): String =
        "DeliveryChallan_${number.replace(Regex("[^A-Za-z0-9_-]"), "_")}.pdf"

    private fun formatDate(value: Long): String =
        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US).format(Date(value))

    private fun formatDateTime(value: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.US).format(Date(value))

    private fun DeliveryChallanStatus.labelRes(): Int = when (this) {
        DeliveryChallanStatus.DRAFT -> R.string.delivery_challan_status_draft
        DeliveryChallanStatus.DISPATCHED -> R.string.delivery_challan_status_dispatched
        DeliveryChallanStatus.CANCELLED -> R.string.delivery_challan_status_cancelled
    }

    private fun pageInfo(pageNumber: Int): PdfDocument.PageInfo =
        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()

    private companion object {
        const val PDF_DIRECTORY = "pdf"
        const val PAGE_WIDTH = 595
        const val PAGE_HEIGHT = 842
        const val HORIZONTAL_MARGIN = 40f
        const val TOP_MARGIN = 42f
        const val FOOTER_MARGIN = 28f
        const val LINE_HEIGHT = 17f
        const val BODY_TEXT_SIZE = 10f
        const val CONTENT_WIDTH = PAGE_WIDTH - HORIZONTAL_MARGIN * 2
        const val CONTENT_BOTTOM = PAGE_HEIGHT - 64f
    }
}
