package com.brandcrafts.erp.data.pdf

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.brandcrafts.erp.R
import com.brandcrafts.erp.domain.model.CompanyConfig
import com.brandcrafts.erp.domain.model.Contact
import com.brandcrafts.erp.domain.model.PurchaseOrder
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PurchaseOrderPdfRenderer @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun render(order: PurchaseOrder, supplier: Contact, company: CompanyConfig): Result<File> =
        withContext(Dispatchers.IO) {
            try {
                require(company.companyName.isNotBlank() && company.addressLine1.isNotBlank() && company.city.isNotBlank() && company.state.isNotBlank() && company.pincode.isNotBlank() && company.country.isNotBlank() && company.phone.isNotBlank() && company.email.isNotBlank())
                val directory = File(context.cacheDir, "pdf")
                check(directory.exists() || directory.mkdirs())
                val file = File(directory, "PurchaseOrder_${order.number.replace(Regex("[^A-Za-z0-9_-]"), "_")}.pdf")
                val document = PdfDocument()
                val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 11f }
                var pageNumber = 1
                var page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                var canvas = page.canvas
                var y = 42f
                fun line(value: String, bold: Boolean = false) { paint.isFakeBoldText = bold; canvas.drawText(value, 40f, y, paint); y += 18f }
                fun date(value: Long) = DateFormat.getDateInstance(DateFormat.MEDIUM, Locale.US).format(Date(value))
                fun header() {
                    line(company.companyName, true)
                    line(listOf(company.addressLine1, company.addressLine2, "${company.city}, ${company.state} ${company.pincode}", company.country).filter(String::isNotBlank).joinToString(" | "))
                    line("${company.phone}  ${company.email}")
                    company.gstNumber.takeIf(String::isNotBlank)?.let { line(context.getString(R.string.purchase_order_pdf_gst_label, it)) }
                    line(context.getString(R.string.purchase_order_pdf_document_title, order.number), true)
                    line(context.getString(R.string.purchase_order_pdf_status_label, context.getString(order.status.labelRes())))
                    line(context.getString(R.string.purchase_order_pdf_supplier_label, supplier.name))
                    supplier.company.takeIf(String::isNotBlank)?.let { line(it) }
                    supplier.phone.takeIf(String::isNotBlank)?.let { line(context.getString(R.string.purchase_order_pdf_phone_label, it)) }
                    supplier.email.takeIf(String::isNotBlank)?.let { line(context.getString(R.string.purchase_order_pdf_email_label, it)) }
                    order.dateMillis?.let { line(context.getString(R.string.purchase_order_pdf_date_label, date(it))) }
                    order.expectedDeliveryDateMillis?.let { line(context.getString(R.string.purchase_order_pdf_expected_delivery_label, date(it))) }
                    line(context.getString(R.string.purchase_order_pdf_table_header), true)
                }
                fun newPage() { document.finishPage(page); pageNumber += 1; page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create()); canvas = page.canvas; y = 42f; header() }
                header()
                order.lines.forEachIndexed { index, item ->
                    if (y > 700f) newPage()
                    line("${index + 1}  ${item.description.take(25)}  ${item.quantity.toPlainString()} ${item.unit}  ${money(item.unitPrice)}  ${money(item.lineTotal)}")
                }
                if (y > 700f) newPage()
                line(context.getString(R.string.purchase_order_pdf_grand_total_label, money(order.total)), true)
                order.remarks.takeIf(String::isNotBlank)?.let { line(context.getString(R.string.purchase_order_pdf_remarks_label, it)) }
                line(context.getString(R.string.purchase_order_pdf_generated_label, date(System.currentTimeMillis())))
                document.finishPage(page)
                file.outputStream().use(document::writeTo)
                document.close()
                Result.success(file)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Throwable) {
                Result.failure(exception)
            }
        }

    private fun money(value: java.math.BigDecimal): String = NumberFormat.getCurrencyInstance(Locale.US).format(value)

    private fun com.brandcrafts.erp.domain.model.PurchaseOrderStatus.labelRes(): Int = when (this) {
        com.brandcrafts.erp.domain.model.PurchaseOrderStatus.DRAFT -> R.string.purchase_order_status_draft
        com.brandcrafts.erp.domain.model.PurchaseOrderStatus.APPROVED -> R.string.purchase_order_status_approved
        com.brandcrafts.erp.domain.model.PurchaseOrderStatus.CANCELLED -> R.string.purchase_order_status_cancelled
    }
}
