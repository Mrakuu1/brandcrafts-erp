package com.brandcrafts.erp.domain.repository
import com.brandcrafts.erp.core.result.StockOutResult
import com.brandcrafts.erp.domain.model.MaterialUsageInput
interface MaterialUsageRepository { suspend fun recordUsage(input:MaterialUsageInput):StockOutResult<Unit> }
