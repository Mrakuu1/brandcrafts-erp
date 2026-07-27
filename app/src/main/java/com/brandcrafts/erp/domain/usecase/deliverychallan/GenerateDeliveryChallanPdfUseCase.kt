package com.brandcrafts.erp.domain.usecase.deliverychallan

import com.brandcrafts.erp.domain.repository.DeliveryChallanPdfRepository
import javax.inject.Inject

class GenerateDeliveryChallanPdfUseCase @Inject constructor(
    private val repository: DeliveryChallanPdfRepository,
) {
    suspend operator fun invoke(challanId: String) = repository.generate(challanId)
}
