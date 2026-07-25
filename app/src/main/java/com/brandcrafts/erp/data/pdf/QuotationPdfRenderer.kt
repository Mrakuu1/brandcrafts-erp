package com.brandcrafts.erp.data.pdf

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.brandcrafts.erp.domain.model.CompanyConfig
import com.brandcrafts.erp.domain.model.Contact
import com.brandcrafts.erp.domain.model.Quotation
import java.io.File
import java.text.NumberFormat
import java.util.Locale

class QuotationPdfRenderer(private val context:Context) {
 suspend fun render(quotation:Quotation, customer:Contact, company:CompanyConfig):Result<File> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { runCatching {
  require(company.companyName.isNotBlank()&&company.addressLine1.isNotBlank()&&company.city.isNotBlank()&&company.state.isNotBlank()&&company.pincode.isNotBlank()&&company.country.isNotBlank()&&company.phone.isNotBlank()&&company.email.isNotBlank())
  val dir=File(context.cacheDir,"pdf"); check(dir.exists()||dir.mkdirs()); val file=File(dir,"Quotation_${quotation.number.replace(Regex("[^A-Za-z0-9_-]"),"_")}.pdf")
  val doc=PdfDocument(); val paint=Paint(Paint.ANTI_ALIAS_FLAG).apply{textSize=11f}; var pageNo=1; var page=doc.startPage(PdfDocument.PageInfo.Builder(595,842,pageNo).create()); var c=page.canvas; var y=48f
  fun text(value:String,bold:Boolean=false){paint.isFakeBoldText=bold;c.drawText(value,40f,y,paint);y+=18f}
  fun newPage(){doc.finishPage(page);pageNo++;page=doc.startPage(PdfDocument.PageInfo.Builder(595,842,pageNo).create());c=page.canvas;y=48f;text("${company.companyName} — ${quotation.number}",true);text("#  Description                 Qty   Unit price    Total",true)}
  text(company.companyName,true); if(company.legalName.isNotBlank())text(company.legalName); text(listOf(company.addressLine1,company.addressLine2,company.city+", "+company.state+" "+company.pincode,company.country).filter(String::isNotBlank).joinToString(" | ")); text("${company.phone}  ${company.email}"); text("QUOTATION ${quotation.number}",true); text("Customer: ${customer.name}"); if(customer.company.isNotBlank())text(customer.company); text("#  Description                 Qty   Unit price    Total",true)
  quotation.lines.forEachIndexed { i,line->if(y>700f)newPage(); text("${i+1}  ${line.description.take(28)}  ${line.quantity.toPlainString()} ${line.unit}  ${NumberFormat.getCurrencyInstance(Locale.getDefault()).format(line.unitPrice)}  ${NumberFormat.getCurrencyInstance().format(line.total)}")}
  if(y>700f)newPage(); text("Grand total: ${NumberFormat.getCurrencyInstance().format(quotation.grandTotal)}",true); if(quotation.remarks.isNotBlank())text("Notes: ${quotation.remarks}"); if(company.quotationTerms.isNotBlank())text("Terms: ${company.quotationTerms}"); if(company.authorizedSignatoryName.isNotBlank())text(company.authorizedSignatoryName); if(company.authorizedSignatoryDesignation.isNotBlank())text(company.authorizedSignatoryDesignation); doc.finishPage(page); file.outputStream().use(doc::writeTo);doc.close();file
 } }
}
