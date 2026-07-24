package com.brandcrafts.erp.domain.usecase

import com.brandcrafts.erp.domain.repository.InventoryRepository
import javax.inject.Inject

class ObserveInventoryItemsUseCase @Inject constructor(
    private val repository: InventoryRepository,
) {
    operator fun invoke() = repository.observeInventoryItems()
}
