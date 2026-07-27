package com.brandcrafts.erp.data.datasource.deliverychallan

interface DeliveryChallanCancellationRemoteDataSource { suspend fun cancelDraft(challanId: String) }
