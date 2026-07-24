package com.brandcrafts.erp.domain.usecase

import com.brandcrafts.erp.domain.model.InventoryItemUpdate
import com.brandcrafts.erp.domain.repository.InventoryRepository
import javax.inject.Inject

class UpdateInventoryItemUseCase @Inject constructor(
    private val repository: InventoryRepository,
) {
    suspend operator fun invoke(input: InventoryItemUpdate) = repository.updateInventoryItem(input)
}
