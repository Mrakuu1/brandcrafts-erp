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

                fun tableHeader() {
                    paint.isFakeBoldText = true
                    canvas.drawText(context.getString(R.string.quotation_pdf_column_number), 40f, y, paint)
                    canvas.drawText(context.getString(R.string.quotation_pdf_column_description), 65f, y, paint)
                    canvas.drawText(context.getString(R.string.quotation_pdf_column_quantity), 300f, y, paint)
                    canvas.drawText(context.getString(R.string.quotation_pdf_column_unit_price), 375f, y, paint)
                    canvas.drawText(context.getString(R.string.quotation_pdf_column_total), 485f, y, paint)
                    y += 18f
                }

                fun newPage() {
                    document.finishPage(page)
                    pageNumber += 1
                    page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                    canvas = page.canvas
                    y = 48f
                    text(context.getString(R.string.quotation_pdf_page_header, company.companyName, quotation.number), true)
                    tableHeader()
                }

                text(company.companyName, true)
                if (company.legalName.isNotBlank()) text(company.legalName)
                text(
                    listOf(
                        company.addressLine1,
                        company.addressLine2,
                        "${company.city}, ${company.state} ${company.pincode}",
                        company.country,
                    ).filter(String::isNotBlank).joinToString(" | "),
                )
                text("${company.phone}  ${company.email}")
                text(context.getString(R.string.quotation_pdf_document_title, quotation.number), true)
                text(context.getString(R.string.quotation_pdf_customer_label, customer.name))
                if (customer.company.isNotBlank()) text(customer.company)
                tableHeader()

                quotation.lines.forEachIndexed { index, line ->
                    if (y > 700f) newPage()
                    paint.isFakeBoldText = false
                    canvas.drawText((index + 1).toString(), 40f, y, paint)
                    canvas.drawText(line.description.take(28), 65f, y, paint)
                    canvas.drawText("${line.quantity.toPlainString()} ${line.unit}", 300f, y, paint)
                    canvas.drawText(formatIndianCurrency(line.unitPrice), 375f, y, paint)
                    canvas.drawText(formatIndianCurrency(line.total), 485f, y, paint)
                    y += 18f
                }
                if (y > 700f) newPage()
                text(context.getString(R.string.quotation_pdf_grand_total_label, formatIndianCurrency(quotation.grandTotal)), true)
                if (quotation.remarks.isNotBlank()) text(context.getString(R.string.quotation_pdf_notes_label, quotation.remarks))
                if (company.quotationTerms.isNotBlank()) text(context.getString(R.string.quotation_pdf_terms_label, company.quotationTerms))
                if (company.authorizedSignatoryName.isNotBlank()) text(company.authorizedSignatoryName)
                if (company.authorizedSignatoryDesignation.isNotBlank()) text(company.authorizedSignatoryDesignation)
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
}
