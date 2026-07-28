package com.brandcrafts.erp.domain.usecase.activity

import com.brandcrafts.erp.domain.repository.ActivityLogRepository
import javax.inject.Inject

class ObserveRecentActivitiesUseCase @Inject constructor(
    private val repository: ActivityLogRepository,
) {
    operator fun invoke(limit: Long = 20) = repository.observeRecentActivities(limit)
}
