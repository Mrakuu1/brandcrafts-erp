package com.brandcrafts.erp.data.pdf

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.brandcrafts.erp.R
import com.brandcrafts.erp.core.format.formatIndianCurrency
import com.brandcrafts.erp.domain.model.CompanyConfig
import com.brandcrafts.erp.domain.model.Contact
import com.brandcrafts.erp.domain.model.Invoice
import com.brandcrafts.erp.domain.model.InvoicePaymentStatus
import com.brandcrafts.erp.domain.model.InvoiceStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.math.BigDecimal
import java.text.DateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class InvoicePdfRenderer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun render(invoice: Invoice, customer: Contact, company: CompanyConfig): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                company.requirePdfIdentity()
                val directory = File(context.cacheDir, PDF_DIRECTORY)
                check(directory.exists() || directory.mkdirs())
                val file = File(directory, invoiceFileName(invoice.number))
                val document = PdfDocument()
                try {
                    renderDocument(document, invoice, customer, company)
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

    private fun renderDocument(
        document: PdfDocument,
        invoice: Invoice,
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
                context.getString(R.string.invoice_pdf_page_label, pageNumber),
                HORIZONTAL_MARGIN,
                PAGE_HEIGHT - FOOTER_MARGIN,
                paint,
            )
        }

        fun drawHeader() {
            drawWrapped(company.companyName, bold = true)
            company.legalName.takeIf(String::isNotBlank)?.let { drawWrapped(it) }
            drawWrapped(
                context.getString(
                    R.string.invoice_pdf_company_address,
                    company.addressLine1,
                    company.addressLine2,
                    company.city,
                    company.state,
                    company.pincode,
                    company.country,
                ),
            )
            drawWrapped(context.getString(R.string.invoice_pdf_company_contact, company.phone, company.email))
            company.gstNumber.takeIf(String::isNotBlank)?.let { drawWrapped(it) }
            drawWrapped(context.getString(R.string.invoice_pdf_document_title, invoice.number), bold = true)
            drawWrapped(context.getString(R.string.invoice_pdf_status_label, context.getString(invoice.status.labelRes())))
            drawWrapped(
                context.getString(
                    R.string.invoice_pdf_payment_status_label,
                    context.getString(invoice.paymentStatus.labelRes()),
                ),
            )
            if (invoice.isOverdue(System.currentTimeMillis())) {
                drawWrapped(context.getString(R.string.invoice_pdf_overdue_label), bold = true)
            }
            drawWrapped(context.getString(R.string.invoice_pdf_customer_label, customer.company.ifBlank { customer.name }))
            customer.phone.takeIf(String::isNotBlank)?.let {
                drawWrapped(context.getString(R.string.invoice_pdf_phone_label, it))
            }
            customer.email.takeIf(String::isNotBlank)?.let {
                drawWrapped(context.getString(R.string.invoice_pdf_email_label, it))
            }
            customer.address.takeIf(String::isNotBlank)?.let {
                drawWrapped(context.getString(R.string.invoice_pdf_customer_address_label, it))
            }
            drawWrapped(context.getString(R.string.invoice_pdf_date_label, formatDate(invoice.invoiceDateMillis)))
            invoice.dueDateMillis?.let {
                drawWrapped(context.getString(R.string.invoice_pdf_due_date_label, formatDate(it)))
            }
            drawWrapped(context.getString(R.string.invoice_pdf_table_header), bold = true)
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
        invoice.lines.sortedBy { it.sortOrder }.forEachIndexed { index, line ->
            if (y > CONTENT_BOTTOM - LINE_HEIGHT * 2) newPage()
            drawWrapped(
                context.getString(
                    R.string.invoice_pdf_table_row,
                    index + 1,
                    line.description,
                    line.quantity.toPlainString(),
                    line.unit,
                    formatCurrency(line.unitPrice),
                    formatPercent(line.discountPercent),
                    formatPercent(line.taxPercent),
                    formatCurrency(line.lineTotal),
                ),
            )
        }
        if (y > CONTENT_BOTTOM - LINE_HEIGHT * 8) newPage()
        drawWrapped(context.getString(R.string.invoice_pdf_subtotal_label, formatCurrency(invoice.subtotal)))
        drawWrapped(context.getString(R.string.invoice_pdf_discount_total_label, formatCurrency(invoice.discountTotal)))
        drawWrapped(context.getString(R.string.invoice_pdf_tax_total_label, formatCurrency(invoice.taxTotal)))
        drawWrapped(context.getString(R.string.invoice_pdf_grand_total_label, formatCurrency(invoice.grandTotal)), bold = true)
        drawWrapped(context.getString(R.string.invoice_pdf_paid_amount_label, formatCurrency(invoice.paidAmount)))
        drawWrapped(context.getString(R.string.invoice_pdf_outstanding_amount_label, formatCurrency(invoice.outstandingAmount)))
        invoice.remarks.takeIf(String::isNotBlank)?.let {
            drawWrapped(context.getString(R.string.invoice_pdf_remarks_label, it))
        }
        drawWrapped(context.getString(R.string.invoice_pdf_generated_label, formatDateTime(System.currentTimeMillis())))
        drawFooter()
        document.finishPage(page)
    }

    private fun pageInfo(pageNumber: Int): PdfDocument.PageInfo =
        PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()

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

    private fun invoiceFileName(invoiceNumber: String): String =
        "Invoice_${invoiceNumber.replace(Regex("[^A-Za-z0-9_-]"), "_")}.pdf"

    private fun formatDate(value: Long): String =
        DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US).format(Date(value))

    private fun formatDateTime(value: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.US).format(Date(value))

    private fun formatCurrency(value: BigDecimal): String =
        formatIndianCurrency(value)

    private fun formatPercent(value: BigDecimal): String =
        context.getString(R.string.invoice_percentage_value, value.toPlainString())

    private fun InvoiceStatus.labelRes(): Int = when (this) {
        InvoiceStatus.DRAFT -> R.string.invoice_status_draft
        InvoiceStatus.ISSUED -> R.string.invoice_status_issued
        InvoiceStatus.CANCELLED -> R.string.invoice_status_cancelled
    }

    private fun InvoicePaymentStatus.labelRes(): Int = when (this) {
        InvoicePaymentStatus.UNPAID -> R.string.invoice_payment_status_unpaid
        InvoicePaymentStatus.PARTIALLY_PAID -> R.string.invoice_payment_status_partially_paid
        InvoicePaymentStatus.PAID -> R.string.invoice_payment_status_paid
    }

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
