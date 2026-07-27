package com.brandcrafts.erp.data.datasource.deliverychallan

import com.brandcrafts.erp.domain.model.DeliveryChallanDispatchRequest

interface DeliveryChallanDispatchRemoteDataSource { suspend fun dispatch(request: DeliveryChallanDispatchRequest) }
