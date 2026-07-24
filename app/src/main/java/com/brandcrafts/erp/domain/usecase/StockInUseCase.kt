package com.brandcrafts.erp.domain.usecase

import com.brandcrafts.erp.domain.model.StockInInput
import com.brandcrafts.erp.domain.repository.StockRepository
import javax.inject.Inject

class StockInUseCase @Inject constructor(private val repository: StockRepository) {
    suspend operator fun invoke(input: StockInInput) = repository.stockIn(input)
}
