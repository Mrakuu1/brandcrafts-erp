package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.data.datasource.activity.ActivityLogRemoteDataSource
import com.brandcrafts.erp.domain.model.ActivityLog
import com.brandcrafts.erp.domain.repository.ActivityLogRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class ActivityLogRepositoryImpl @Inject constructor(
    private val remoteDataSource: ActivityLogRemoteDataSource,
) : ActivityLogRepository {
    override fun observeRecentActivities(limit: Long): Flow<Result<List<ActivityLog>>> =
        remoteDataSource.observeRecentActivities(limit)
            .map { Result.success(it) }
            .catch { exception ->
                if (exception is CancellationException) throw exception
                emit(Result.failure(exception))
            }
}
