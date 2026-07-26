package com.brandcrafts.erp.domain.usecase.purchaseorder

import com.brandcrafts.erp.domain.model.PurchaseOrderDraft
import com.brandcrafts.erp.domain.repository.PurchaseOrderRepository
import javax.inject.Inject

class ObservePurchaseOrdersUseCase @Inject constructor(private val repository: PurchaseOrderRepository) { operator fun invoke() = repository.observePurchaseOrders() }
class GetPurchaseOrderUseCase @Inject constructor(private val repository: PurchaseOrderRepository) { suspend operator fun invoke(id: String) = repository.getPurchaseOrder(id) }
class CreatePurchaseOrderUseCase @Inject constructor(private val repository: PurchaseOrderRepository) { suspend operator fun invoke(draft: PurchaseOrderDraft) = repository.createPurchaseOrder(draft) }
class UpdatePurchaseOrderUseCase @Inject constructor(private val repository: PurchaseOrderRepository) { suspend operator fun invoke(id: String, draft: PurchaseOrderDraft) = repository.updatePurchaseOrder(id, draft) }
class ApprovePurchaseOrderUseCase @Inject constructor(private val repository: PurchaseOrderRepository) { suspend operator fun invoke(id: String) = repository.approvePurchaseOrder(id) }
class CancelPurchaseOrderUseCase @Inject constructor(private val repository: PurchaseOrderRepository) { suspend operator fun invoke(id: String) = repository.cancelPurchaseOrder(id) }
