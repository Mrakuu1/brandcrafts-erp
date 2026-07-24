package com.brandcrafts.erp.domain.usecase
import com.brandcrafts.erp.domain.model.MaterialUsageInput
import com.brandcrafts.erp.domain.repository.MaterialUsageRepository
import javax.inject.Inject
class RecordMaterialUsageUseCase @Inject constructor(private val repository:MaterialUsageRepository){suspend operator fun invoke(input:MaterialUsageInput)=repository.recordUsage(input)}
