package com.brandcrafts.erp.domain.usecase

import com.brandcrafts.erp.domain.repository.InventoryRepository
import javax.inject.Inject

class GetInventoryItemUseCase @Inject constructor(
    private val repository: InventoryRepository,
) {
    suspend operator fun invoke(id: String) = repository.getInventoryItem(id)
}
