package com.brandcrafts.erp.domain.repository

import com.brandcrafts.erp.domain.model.DeliveryChallan
import com.brandcrafts.erp.domain.model.DeliveryChallanCreateRequest
import com.brandcrafts.erp.domain.model.DeliveryChallanDraftUpdateRequest
import com.brandcrafts.erp.domain.model.DeliveryChallanDispatchRequest
import com.brandcrafts.erp.domain.model.DeliveryChallanInvoiceCreateRequest
import com.brandcrafts.erp.domain.model.DeliveryChallanSummary
import com.brandcrafts.erp.domain.model.DeliveryChallanLine
import kotlinx.coroutines.flow.Flow

interface DeliveryChallanRepository {
    fun observeDeliveryChallans(): Flow<Result<List<DeliveryChallanSummary>>>
    suspend fun getDeliveryChallan(challanId: String): Result<DeliveryChallan>
    suspend fun getDeliveryChallanLines(challanId: String): Result<List<DeliveryChallanLine>>
    suspend fun createDeliveryChallan(request: DeliveryChallanCreateRequest): Result<String>
    suspend fun createDeliveryChallanFromInvoice(request: DeliveryChallanInvoiceCreateRequest): Result<String>
    suspend fun updateDraftDeliveryChallan(request: DeliveryChallanDraftUpdateRequest): Result<Unit>
    suspend fun dispatchDeliveryChallan(request: DeliveryChallanDispatchRequest): Result<Unit>
    suspend fun cancelDraftDeliveryChallan(challanId: String): Result<Unit>
}
