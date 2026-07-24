package com.brandcrafts.erp.data.repository

import com.brandcrafts.erp.core.common.CurrentUserState
import com.brandcrafts.erp.core.common.SessionManager
import com.brandcrafts.erp.core.result.InventoryError
import com.brandcrafts.erp.core.result.InventoryResult
import com.brandcrafts.erp.data.datasource.inventory.InventoryRemoteDataSource
import com.brandcrafts.erp.data.mapper.toDomain
import com.brandcrafts.erp.data.mapper.toFirestoreInventoryItem
import com.brandcrafts.erp.domain.model.AuthenticatedUser
import com.brandcrafts.erp.domain.model.InventoryItem
import com.brandcrafts.erp.domain.model.InventoryItemInput
import com.brandcrafts.erp.domain.model.InventoryItemUpdate
import com.brandcrafts.erp.domain.model.UserRole
import com.brandcrafts.erp.domain.repository.InventoryRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestoreException
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class InventoryRepositoryImpl @Inject constructor(
    private val remoteDataSource: InventoryRemoteDataSource,
    private val sessionManager: SessionManager,
) : InventoryRepository {

    override fun observeInventoryItems(): Flow<InventoryResult<List<InventoryItem>>> {
        if (activeUser() == null) return flowOf(InventoryResult.Error(InventoryError.UNAUTHORIZED))

        return remoteDataSource.observeItems()
            .map { items -> InventoryResult.Success(items.map { it.toDomain() }) as InventoryResult<List<InventoryItem>> }
            .catch { throwable -> emit(InventoryResult.Error(throwable.toInventoryError())) }
    }

    override suspend fun getInventoryItem(id: String): InventoryResult<InventoryItem> {
        if (id.isBlank()) return InventoryResult.Error(InventoryError.VALIDATION_FAILED)
        if (activeUser() == null) return InventoryResult.Error(InventoryError.UNAUTHORIZED)

        return runCatching { remoteDataSource.getItem(id)?.toDomain() }
            .fold(
                onSuccess = { item ->
                    item?.let { InventoryResult.Success(it) }
                        ?: InventoryResult.Error(InventoryError.ITEM_NOT_FOUND)
                },
                onFailure = { throwable -> InventoryResult.Error(throwable.toInventoryError()) },
            )
    }

    override suspend fun createInventoryItem(input: InventoryItemInput): InventoryResult<Unit> {
        if (!input.isValid()) return InventoryResult.Error(InventoryError.VALIDATION_FAILED)
        val user = adminUser() ?: return InventoryResult.Error(InventoryError.UNAUTHORIZED)

        return runCatching {
            if (remoteDataSource.isSkuInUse(input.sku.trim())) {
                throw DuplicateSkuException
            }
            remoteDataSource.createItem(input.normalized().toFirestoreInventoryItem(user.uid))
        }.fold(
            onSuccess = { InventoryResult.Success(Unit) },
            onFailure = { throwable -> InventoryResult.Error(throwable.toInventoryError()) },
        )
    }

    override suspend fun updateInventoryItem(input: InventoryItemUpdate): InventoryResult<Unit> {
        if (!input.isValid()) return InventoryResult.Error(InventoryError.VALIDATION_FAILED)
        val user = adminUser() ?: return InventoryResult.Error(InventoryError.UNAUTHORIZED)

        return runCatching {
            if (remoteDataSource.isSkuInUse(input.sku.trim(), input.id)) {
                throw DuplicateSkuException
            }
            remoteDataSource.updateItem(input.normalized().toFirestoreInventoryItem(user.uid))
        }.fold(
            onSuccess = { InventoryResult.Success(Unit) },
            onFailure = { throwable -> InventoryResult.Error(throwable.toInventoryError()) },
        )
    }

    override fun searchInventoryItems(query: String): Flow<InventoryResult<List<InventoryItem>>> {
        val normalizedQuery = query.trim()
        return observeInventoryItems().map { result ->
            when (result) {
                is InventoryResult.Success -> InventoryResult.Success(
                    result.data.filter { item ->
                        normalizedQuery.isBlank() ||
                            item.name.contains(normalizedQuery, ignoreCase = true) ||
                            item.sku.contains(normalizedQuery, ignoreCase = true) ||
                            item.category.contains(normalizedQuery, ignoreCase = true)
                    },
                )
                is InventoryResult.Error -> result
            }
        }
    }

    private fun activeUser(): AuthenticatedUser? = (sessionManager.currentUser.value as? CurrentUserState.Authenticated)
        ?.user
        ?.takeIf { it.active }

    private fun adminUser(): AuthenticatedUser? = activeUser()?.takeIf { it.role == UserRole.ADMIN }

    private fun InventoryItemInput.isValid(): Boolean = name.isNotBlank() &&
        sku.isNotBlank() &&
        category.isNotBlank() &&
        unit.isNotBlank() &&
        availableQuantity >= 0 &&
        minimumQuantity >= 0 &&
        purchasePrice >= 0 &&
        sellingPrice >= 0

    private fun InventoryItemUpdate.isValid(): Boolean = id.isNotBlank() &&
        name.isNotBlank() &&
        sku.isNotBlank() &&
        category.isNotBlank() &&
        unit.isNotBlank() &&
        availableQuantity >= 0 &&
        minimumQuantity >= 0 &&
        purchasePrice >= 0 &&
        sellingPrice >= 0

    private fun InventoryItemInput.normalized(): InventoryItemInput = copy(
        name = name.trim(),
        sku = sku.trim(),
        category = category.trim(),
        unit = unit.trim(),
        description = description.trim(),
    )

    private fun InventoryItemUpdate.normalized(): InventoryItemUpdate = copy(
        id = id.trim(),
        name = name.trim(),
        sku = sku.trim(),
        category = category.trim(),
        unit = unit.trim(),
        description = description.trim(),
    )

    private fun Throwable.toInventoryError(): InventoryError = when (this) {
        DuplicateSkuException -> InventoryError.VALIDATION_FAILED
        is FirebaseNetworkException -> InventoryError.NETWORK_UNAVAILABLE
        is FirebaseFirestoreException -> when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> InventoryError.UNAUTHORIZED
            FirebaseFirestoreException.Code.NOT_FOUND -> InventoryError.ITEM_NOT_FOUND
            FirebaseFirestoreException.Code.UNAVAILABLE -> InventoryError.NETWORK_UNAVAILABLE
            else -> InventoryError.UNKNOWN
        }
        else -> InventoryError.UNKNOWN
    }

    private data object DuplicateSkuException : IllegalStateException()
}
