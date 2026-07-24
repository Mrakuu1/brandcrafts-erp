package com.brandcrafts.erp.data.datasource.stock

import com.brandcrafts.erp.data.model.FirestoreStockIn

interface StockRemoteDataSource { suspend fun stockIn(input: FirestoreStockIn) }
