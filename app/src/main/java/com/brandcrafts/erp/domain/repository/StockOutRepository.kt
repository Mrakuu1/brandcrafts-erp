package com.brandcrafts.erp.domain.repository
import com.brandcrafts.erp.core.result.StockOutResult
import com.brandcrafts.erp.domain.model.StockOutInput
interface StockOutRepository { suspend fun stockOut(input: StockOutInput): StockOutResult<Unit> }
