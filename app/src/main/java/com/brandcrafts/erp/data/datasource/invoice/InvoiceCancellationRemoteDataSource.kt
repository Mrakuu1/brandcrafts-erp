package com.brandcrafts.erp.data.datasource.invoice

interface InvoiceCancellationRemoteDataSource { suspend fun cancelInvoice(invoiceId: String) }
