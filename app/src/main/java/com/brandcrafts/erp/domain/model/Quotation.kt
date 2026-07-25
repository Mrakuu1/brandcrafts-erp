package com.brandcrafts.erp.domain.model
import java.math.BigDecimal
data class Quotation(val id:String,val number:String,val contactId:String,val dateMillis:Long?,val validUntilMillis:Long?,val status:QuotationStatus,val grandTotal:BigDecimal,val pdfUrl:String,val createdBy:String,val remarks:String,val lines:List<QuotationLineItem> = emptyList())
data class QuotationLineItem(val id:String,val materialId:String,val description:String,val quantity:BigDecimal,val unit:String,val unitPrice:BigDecimal,val discount:BigDecimal,val tax:BigDecimal,val total:BigDecimal)
data class QuotationDraft(val contactId:String,val validUntilMillis:Long?,val remarks:String,val lines:List<QuotationDraftLine>)
data class QuotationDraftLine(val id:String?=null,val materialId:String,val description:String,val quantity:BigDecimal,val unit:String,val unitPrice:BigDecimal,val discountPercent:BigDecimal,val taxPercent:BigDecimal)
enum class QuotationStatus { DRAFT, APPROVED, REJECTED, EXPIRED }
