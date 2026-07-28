package com.brandcrafts.erp.domain.repository

import com.brandcrafts.erp.domain.model.ActivityLog
import kotlinx.coroutines.flow.Flow

interface ActivityLogRepository {
    fun observeRecentActivities(limit: Long = 20): Flow<Result<List<ActivityLog>>>
}
