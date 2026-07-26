package com.brandcrafts.erp.data.repository

data class InvoiceActivityPayload(val id: String, val action: String, val referenceId: String, val performedBy: String, val performedByName: String) {
    fun asFields() = mapOf("id" to id, "module" to "INVOICES", "action" to action, "referenceId" to referenceId, "referenceType" to "INVOICE", "description" to action, "performedBy" to performedBy, "performedByName" to performedByName)
}
internal object InvoiceActivityFactory { fun created(id:String,uid:String,name:String)=payload(id,"INVOICE_CREATED",uid,name); fun updated(id:String,uid:String,name:String)=payload(id,"INVOICE_UPDATED",uid,name); fun issued(id:String,uid:String,name:String)=payload(id,"INVOICE_ISSUED",uid,name); fun cancelled(id:String,uid:String,name:String)=payload(id,"INVOICE_CANCELLED",uid,name); fun paymentRecorded(id:String,uid:String,name:String)=payload(id,"INVOICE_PAYMENT_RECORDED",uid,name); private fun payload(id:String,action:String,uid:String,name:String)=InvoiceActivityPayload(id,action,id,uid,name) }
