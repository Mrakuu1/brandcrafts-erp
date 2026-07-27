package com.brandcrafts.erp.data.datasource.deliverychallan

import com.brandcrafts.erp.data.model.deliverychallan.DeliveryChallanDto
import com.brandcrafts.erp.data.model.deliverychallan.DeliveryChallanLineDto
import kotlinx.coroutines.flow.Flow

interface DeliveryChallanRemoteDataSource {
    fun observeParents(): Flow<List<DeliveryChallanDto>>
    suspend fun getParent(challanId: String): DeliveryChallanDto?
    suspend fun getLines(challanId: String): List<DeliveryChallanLineDto>
}
