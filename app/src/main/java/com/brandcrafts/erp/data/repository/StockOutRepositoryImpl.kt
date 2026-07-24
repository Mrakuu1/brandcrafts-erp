package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.core.result.StockOutError
import com.brandcrafts.erp.core.result.StockOutResult
import com.brandcrafts.erp.data.datasource.stock.InsufficientStockException
import com.brandcrafts.erp.data.datasource.stock.StockOutMaterialInactiveException
import com.brandcrafts.erp.data.datasource.stock.StockOutMaterialNotFoundException
import com.brandcrafts.erp.data.datasource.stock.StockOutRemoteDataSource
import com.brandcrafts.erp.domain.model.StockOutInput
import com.brandcrafts.erp.domain.repository.StockOutRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import javax.inject.Inject

class StockOutRepositoryImpl @Inject constructor(private val remote: StockOutRemoteDataSource, private val sessionManager: SessionManager) : StockOutRepository {
    override suspend fun stockOut(input: StockOutInput): StockOutResult<Unit> {
        if (input.materialId.isBlank() || input.quantity <= 0) return StockOutResult.Error(StockOutError.VALIDATION_FAILED)
        val user = (sessionManager.currentUser.value as? CurrentUserState.Authenticated)?.user?.takeIf { it.active } ?: return StockOutResult.Error(StockOutError.UNAUTHORIZED)
        return try { remote.stockOut(input, user); StockOutResult.Success(Unit) } catch (error: Throwable) { StockOutResult.Error(error.toStockOutError()) }
    }
    private fun Throwable.toStockOutError() = when (this) {
        StockOutMaterialNotFoundException -> StockOutError.MATERIAL_NOT_FOUND
        StockOutMaterialInactiveException -> StockOutError.MATERIAL_INACTIVE
        InsufficientStockException -> StockOutError.INSUFFICIENT_STOCK
        is FirebaseNetworkException -> StockOutError.NETWORK_UNAVAILABLE
        is FirebaseFirestoreException -> when (code) { FirebaseFirestoreException.Code.PERMISSION_DENIED -> StockOutError.UNAUTHORIZED; FirebaseFirestoreException.Code.UNAVAILABLE -> StockOutError.NETWORK_UNAVAILABLE; else -> StockOutError.UNKNOWN }
        else -> StockOutError.UNKNOWN
    }
}
