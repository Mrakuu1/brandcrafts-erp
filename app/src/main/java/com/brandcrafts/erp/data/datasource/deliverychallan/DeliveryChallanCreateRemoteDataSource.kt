package com.brandcrafts.erp.data.datasource.deliverychallan

import com.brandcrafts.erp.domain.model.DeliveryChallanCreateRequest

interface DeliveryChallanCreateRemoteDataSource {
    suspend fun createIndependent(request: DeliveryChallanCreateRequest): String
}
