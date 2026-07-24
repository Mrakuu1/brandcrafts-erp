package com.brandcrafts.erp.domain.usecase
import com.brandcrafts.erp.domain.repository.InventoryTransactionRepository
import javax.inject.Inject
class ObserveRecentInventoryTransactionsUseCase @Inject constructor(private val r:InventoryTransactionRepository){operator fun invoke(materialId:String)=r.observeRecent(materialId)}
