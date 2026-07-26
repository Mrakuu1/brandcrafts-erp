package com.brandcrafts.erp.data.datasource.invoice

interface InvoiceIssueRemoteDataSource { suspend fun issueInvoice(invoiceId: String) }
