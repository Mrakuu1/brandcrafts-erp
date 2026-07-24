package com.brandcrafts.erp.domain.usecase

import com.brandcrafts.erp.domain.model.InventoryItemInput
import com.brandcrafts.erp.domain.repository.InventoryRepository
import javax.inject.Inject

class CreateInventoryItemUseCase @Inject constructor(
    private val repository: InventoryRepository,
) {
    suspend operator fun invoke(input: InventoryItemInput) = repository.createInventoryItem(input)
}
