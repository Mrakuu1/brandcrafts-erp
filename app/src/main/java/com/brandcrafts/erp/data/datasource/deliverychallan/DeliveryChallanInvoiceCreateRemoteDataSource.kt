package com.brandcrafts.erp.data.datasource.deliverychallan

import com.brandcrafts.erp.domain.model.DeliveryChallanInvoiceCreateRequest

interface DeliveryChallanInvoiceCreateRemoteDataSource {
    suspend fun createFromInvoice(request: DeliveryChallanInvoiceCreateRequest): String
}
