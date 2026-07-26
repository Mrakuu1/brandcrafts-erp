package com.brandcrafts.erp.data.datasource.invoice

import com.brandcrafts.erp.domain.model.InvoiceDraftUpdateRequest

interface InvoiceDraftUpdateRemoteDataSource { suspend fun updateDraftInvoice(request: InvoiceDraftUpdateRequest) }
