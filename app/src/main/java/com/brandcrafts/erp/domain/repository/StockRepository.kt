package com.brandcrafts.erp.domain.repository

import com.brandcrafts.erp.core.result.StockResult
import com.brandcrafts.erp.domain.model.StockInInput

interface StockRepository { suspend fun stockIn(input: StockInInput): StockResult<Unit> }
