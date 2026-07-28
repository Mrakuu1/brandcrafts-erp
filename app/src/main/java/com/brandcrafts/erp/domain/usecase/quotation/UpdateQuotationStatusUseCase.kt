package com.brandcrafts.erp.domain.usecase.quotation

import com.brandcrafts.erp.domain.model.QuotationStatus
import com.brandcrafts.erp.domain.repository.QuotationRepository
import javax.inject.Inject

class UpdateQuotationStatusUseCase @Inject constructor(
    private val repository: QuotationRepository,
) {
    suspend operator fun invoke(id: String, status: QuotationStatus): Result<Unit> =
        repository.updateQuotationStatus(id, status)
}
