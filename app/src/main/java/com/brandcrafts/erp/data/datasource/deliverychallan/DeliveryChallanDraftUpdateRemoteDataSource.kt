package com.brandcrafts.erp.data.datasource.deliverychallan

import com.brandcrafts.erp.domain.model.DeliveryChallanDraftUpdateRequest

interface DeliveryChallanDraftUpdateRemoteDataSource { suspend fun updateDraft(request: DeliveryChallanDraftUpdateRequest) }
