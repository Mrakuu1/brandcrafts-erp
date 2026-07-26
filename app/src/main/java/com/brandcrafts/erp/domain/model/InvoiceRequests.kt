package com.brandcrafts.erp.domain.model

import java.math.BigDecimal

data class InvoiceCreateRequest(val customerId: String, val invoiceDateMillis: Long, val dueDateMillis: Long?, val lines: List<InvoiceLine>, val remarks: String)
data class InvoiceDraftUpdateRequest(val invoiceId: String, val customerId: String, val invoiceDateMillis: Long, val dueDateMillis: Long?, val lines: List<InvoiceDraftUpdateLine>, val remarks: String)
data class InvoiceDraftUpdateLine(val persistedLineId: String?, val materialId: String, val description: String, val quantity: BigDecimal, val unit: String, val unitPrice: BigDecimal, val discountPercent: BigDecimal, val taxPercent: BigDecimal)
data class InvoicePaymentRequest(val invoiceId: String, val amount: BigDecimal)
