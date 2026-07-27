package com.brandcrafts.erp.domain.usecase.deliverychallan

import com.brandcrafts.erp.domain.model.DeliveryChallan
import com.brandcrafts.erp.domain.model.DeliveryChallanCreateRequest
import com.brandcrafts.erp.domain.model.DeliveryChallanDispatchRequest
import com.brandcrafts.erp.domain.model.DeliveryChallanDraftUpdateRequest
import com.brandcrafts.erp.domain.model.DeliveryChallanInvoiceCreateRequest
import com.brandcrafts.erp.domain.model.DeliveryChallanLine
import com.brandcrafts.erp.domain.model.DeliveryChallanSummary
import com.brandcrafts.erp.domain.repository.DeliveryChallanRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow

class ObserveDeliveryChallansUseCase @Inject constructor(private val repository: DeliveryChallanRepository) { operator fun invoke(): Flow<Result<List<DeliveryChallanSummary>>> = repository.observeDeliveryChallans() }
class GetDeliveryChallanUseCase @Inject constructor(private val repository: DeliveryChallanRepository) { suspend operator fun invoke(id: String): Result<DeliveryChallan> = repository.getDeliveryChallan(id) }
class GetDeliveryChallanLinesUseCase @Inject constructor(private val repository: DeliveryChallanRepository) { suspend operator fun invoke(id: String): Result<List<DeliveryChallanLine>> = repository.getDeliveryChallanLines(id) }
class CreateIndependentDeliveryChallanUseCase @Inject constructor(private val repository: DeliveryChallanRepository) { suspend operator fun invoke(request: DeliveryChallanCreateRequest): Result<String> = repository.createDeliveryChallan(request) }
class CreateDeliveryChallanFromInvoiceUseCase @Inject constructor(private val repository: DeliveryChallanRepository) { suspend operator fun invoke(request: DeliveryChallanInvoiceCreateRequest): Result<String> = repository.createDeliveryChallanFromInvoice(request) }
class UpdateDraftDeliveryChallanUseCase @Inject constructor(private val repository: DeliveryChallanRepository) { suspend operator fun invoke(request: DeliveryChallanDraftUpdateRequest): Result<Unit> = repository.updateDraftDeliveryChallan(request) }
class DispatchDeliveryChallanUseCase @Inject constructor(private val repository: DeliveryChallanRepository) { suspend operator fun invoke(request: DeliveryChallanDispatchRequest): Result<Unit> = repository.dispatchDeliveryChallan(request) }
class CancelDraftDeliveryChallanUseCase @Inject constructor(private val repository: DeliveryChallanRepository) { suspend operator fun invoke(id: String): Result<Unit> = repository.cancelDraftDeliveryChallan(id) }
