package com.brandcrafts.erp.domain.usecase
import com.brandcrafts.erp.domain.model.StockOutInput
import com.brandcrafts.erp.domain.repository.StockOutRepository
import javax.inject.Inject
class StockOutUseCase @Inject constructor(private val repository: StockOutRepository) { suspend operator fun invoke(input: StockOutInput) = repository.stockOut(input) }
