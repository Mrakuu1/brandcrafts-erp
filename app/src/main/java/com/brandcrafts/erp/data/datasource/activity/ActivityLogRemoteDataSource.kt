package com.brandcrafts.erp.data.datasource.activity

import com.brandcrafts.erp.domain.model.ActivityLog
import kotlinx.coroutines.flow.Flow

interface ActivityLogRemoteDataSource {
    fun observeRecentActivities(limit: Long): Flow<List<ActivityLog>>
}
