package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.core.result.StockError
import com.brandcrafts.erp.core.result.StockResult
import com.brandcrafts.erp.data.datasource.stock.MaterialInactiveException
import com.brandcrafts.erp.data.datasource.stock.MaterialNotFoundException
import com.brandcrafts.erp.data.datasource.stock.StockRemoteDataSource
import com.brandcrafts.erp.data.mapper.toFirestoreStockIn
import com.brandcrafts.erp.domain.model.StockInInput
import com.brandcrafts.erp.domain.repository.InventoryRepository
import com.brandcrafts.erp.domain.repository.StockRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import javax.inject.Inject

class StockRepositoryImpl @Inject constructor(
    private val remoteDataSource: StockRemoteDataSource,
    private val inventoryRepository: InventoryRepository,
    private val sessionManager: SessionManager,
) : StockRepository {
    override suspend fun stockIn(input: StockInInput): StockResult<Unit> {
        if (input.materialId.isBlank() || input.quantity <= 0) return StockResult.Error(StockError.VALIDATION_FAILED)
        val user = (sessionManager.currentUser.value as? CurrentUserState.Authenticated)?.user
            ?.takeIf { it.active } ?: return StockResult.Error(StockError.UNAUTHORIZED)
        return try {
            val material = when (val result = inventoryRepository.getInventoryItem(input.materialId)) {
                is com.brandcrafts.erp.core.result.InventoryResult.Success -> result.data
                is com.brandcrafts.erp.core.result.InventoryResult.Error -> return StockResult.Error(StockError.MATERIAL_NOT_FOUND)
            }
            remoteDataSource.stockIn(input.toFirestoreStockIn(user, material.unit))
            StockResult.Success(Unit)
        } catch (throwable: Throwable) { StockResult.Error(throwable.toStockError()) }
    }

    private fun Throwable.toStockError(): StockError = when (this) {
        MaterialNotFoundException -> StockError.MATERIAL_NOT_FOUND
        MaterialInactiveException -> StockError.MATERIAL_INACTIVE
        is FirebaseNetworkException -> StockError.NETWORK_UNAVAILABLE
        is FirebaseFirestoreException -> when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> StockError.UNAUTHORIZED
            FirebaseFirestoreException.Code.NOT_FOUND -> StockError.MATERIAL_NOT_FOUND
            FirebaseFirestoreException.Code.UNAVAILABLE -> StockError.NETWORK_UNAVAILABLE
            else -> StockError.UNKNOWN
        }
        else -> StockError.UNKNOWN
    }
}
