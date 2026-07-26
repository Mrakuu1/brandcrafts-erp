package com.brandcrafts.erp.data.datasource.invoice

import com.brandcrafts.erp.domain.model.InvoicePaymentRequest

interface InvoicePaymentRemoteDataSource { suspend fun recordPayment(request: InvoicePaymentRequest) }
